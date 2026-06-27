package com.testetecnico.tarifaagua;

import com.testetecnico.tarifaagua.domain.Categoria;
import com.testetecnico.tarifaagua.domain.FaixaConsumo;
import com.testetecnico.tarifaagua.dto.CalculoResponse;
import com.testetecnico.tarifaagua.dto.TabelaResponse;
import com.testetecnico.tarifaagua.repository.FaixaConsumoRepository;
import com.testetecnico.tarifaagua.repository.TabelaTarifariaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Testes de aceite ponta a ponta — um por critério do desafio.
 *
 * <p>Exercitam toda a pilha (controller → service → repository → JPA) contra um
 * banco <strong>H2 em memória</strong>, sem dependências externas. Cada teste
 * cria os próprios dados via API; o {@code @BeforeEach} limpa a base, garantindo
 * isolamento.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Critérios de Aceite (integração com H2 em memória)")
class CriteriosDeAceiteIT {

    private static final String TABELAS = "/api/tabelas-tarifarias";
    private static final String CALCULOS = "/api/calculos";

    /** Tabela completa (4 categorias). A INDUSTRIAL: 18 m³... 20 m³ → R$ 40,00. */
    private static final String TABELA_COMPLETA = """
            {
              "nome": "Tabela de Teste",
              "dataVigencia": "2030-01-01",
              "categorias": [
                { "categoria": "COMERCIAL",  "faixas": [ {"inicio":0,"fim":10,"valorUnitario":1.50}, {"inicio":11,"fim":99999,"valorUnitario":2.50} ] },
                { "categoria": "INDUSTRIAL", "faixas": [ {"inicio":0,"fim":10,"valorUnitario":1.50}, {"inicio":11,"fim":30,"valorUnitario":2.50}, {"inicio":31,"fim":99999,"valorUnitario":3.50} ] },
                { "categoria": "PARTICULAR", "faixas": [ {"inicio":0,"fim":10,"valorUnitario":0.80}, {"inicio":11,"fim":99999,"valorUnitario":1.60} ] },
                { "categoria": "PUBLICO",    "faixas": [ {"inicio":0,"fim":10,"valorUnitario":1.20}, {"inicio":11,"fim":99999,"valorUnitario":2.20} ] }
              ]
            }
            """;

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private TabelaTarifariaRepository tabelaRepository;

    @Autowired
    private FaixaConsumoRepository faixaRepository;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
        faixaRepository.deleteAll();
        tabelaRepository.deleteAll();
    }

    private TabelaResponse criarTabela() {
        return client.post().uri(TABELAS)
                .contentType(MediaType.APPLICATION_JSON)
                .body(TABELA_COMPLETA)
                .retrieve()
                .body(TabelaResponse.class);
    }

    private CalculoResponse calcular(String categoria, int consumo) {
        return client.post().uri(CALCULOS)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"categoria\":\"" + categoria + "\",\"consumo\":" + consumo + "}")
                .retrieve()
                .body(CalculoResponse.class);
    }

    @Test
    @DisplayName("Critério 1: cria tabela completa com as quatro categorias e faixas")
    void criterio1_criarTabelaCompleta() {
        TabelaResponse criada = criarTabela();

        assertThat(criada).isNotNull();
        assertThat(criada.id()).isNotNull();
        assertThat(criada.ativo()).isTrue();
        assertThat(criada.categorias()).hasSize(4);
    }

    @Test
    @DisplayName("Critério 2: lista as tabelas cadastradas")
    void criterio2_listar() {
        Long id = criarTabela().id();

        List<TabelaResponse> lista = client.get().uri(TABELAS)
                .retrieve()
                .body(new ParameterizedTypeReference<List<TabelaResponse>>() {
                });

        assertThat(lista).extracting(TabelaResponse::id).contains(id);
    }

    @Test
    @DisplayName("Critério 3: calcula o valor progressivo (INDUSTRIAL 20 m³ = R$ 40,00)")
    void criterio3_calculo() {
        criarTabela();

        CalculoResponse r = calcular("INDUSTRIAL", 20);

        assertThat(r.consumoTotal()).isEqualTo(20);
        assertThat(r.valorTotal()).isEqualByComparingTo("40.00");
        assertThat(r.detalhamento()).hasSize(2);
        assertThat(r.detalhamento().get(0).subtotal()).isEqualByComparingTo("15.00");
        assertThat(r.detalhamento().get(1).subtotal()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("Critério 4: parametrização — alterar valor no banco muda o cálculo, sem alterar código")
    void criterio4_parametrizacao() {
        criarTabela();

        // Antes: 20 m³ = R$ 40,00
        assertThat(calcular("INDUSTRIAL", 20).valorTotal()).isEqualByComparingTo("40.00");

        // Altera no banco o valor da faixa 0–10 (1,50 -> 2,50)
        FaixaConsumo primeira = faixaRepository.findVigentesPorCategoria(Categoria.INDUSTRIAL).stream()
                .filter(f -> f.getInicio() == 0)
                .findFirst()
                .orElseThrow();
        primeira.setValorUnitario(new BigDecimal("2.50"));
        faixaRepository.save(primeira);

        // Depois: 20 m³ = R$ 50,00 (10×2,50 + 10×2,50)
        assertThat(calcular("INDUSTRIAL", 20).valorTotal()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("Exclusão lógica: tabela excluída some da listagem; id inexistente retorna 404")
    void exclusaoLogica() {
        Long id = criarTabela().id();

        client.delete().uri(TABELAS + "/" + id).retrieve().toBodilessEntity();

        List<TabelaResponse> lista = client.get().uri(TABELAS)
                .retrieve()
                .body(new ParameterizedTypeReference<List<TabelaResponse>>() {
                });
        assertThat(lista).noneMatch(t -> t.id().equals(id));

        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> client.delete().uri(TABELAS + "/999999").retrieve().toBodilessEntity())
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
