package com.testetecnico.tarifaagua.service;

import com.testetecnico.tarifaagua.domain.Categoria;
import com.testetecnico.tarifaagua.domain.FaixaConsumo;
import com.testetecnico.tarifaagua.domain.TabelaTarifaria;
import com.testetecnico.tarifaagua.dto.CalculoRequest;
import com.testetecnico.tarifaagua.dto.CalculoResponse;
import com.testetecnico.tarifaagua.exception.RegraNegocioException;
import com.testetecnico.tarifaagua.repository.FaixaConsumoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CalculoService - cálculo progressivo por faixas")
class CalculoServiceTest {

    @Mock
    private FaixaConsumoRepository faixaRepository;

    @InjectMocks
    private CalculoService service;

    private TabelaTarifaria tabela;

    @BeforeEach
    void setUp() {
        tabela = new TabelaTarifaria();
        tabela.setId(1L);
    }

    private FaixaConsumo faixa(int inicio, int fim, String valor) {
        FaixaConsumo f = new FaixaConsumo();
        f.setTabela(tabela);
        f.setCategoria(Categoria.INDUSTRIAL);
        f.setInicio(inicio);
        f.setFim(fim);
        f.setValorUnitario(new BigDecimal(valor));
        return f;
    }

    private List<FaixaConsumo> faixasIndustrial() {
        return List.of(
                faixa(0, 10, "1.00"),
                faixa(11, 20, "2.00"),
                faixa(21, 30, "3.00"),
                faixa(31, 99999, "4.00"));
    }

    @Test
    @DisplayName("Caso do desafio: 18 m³ = R$ 26,00")
    void deveCalcularCasoCanonico() {
        when(faixaRepository.findVigentesPorCategoria(Categoria.INDUSTRIAL)).thenReturn(faixasIndustrial());

        CalculoResponse r = service.calcular(new CalculoRequest(Categoria.INDUSTRIAL, 18));

        assertThat(r.valorTotal()).isEqualByComparingTo("26.00");
        assertThat(r.detalhamento()).hasSize(2);
        assertThat(r.detalhamento().get(0).m3Cobrados()).isEqualTo(10);
        assertThat(r.detalhamento().get(0).subtotal()).isEqualByComparingTo("10.00");
        assertThat(r.detalhamento().get(1).m3Cobrados()).isEqualTo(8);
        assertThat(r.detalhamento().get(1).subtotal()).isEqualByComparingTo("16.00");
    }

    @Test
    @DisplayName("Consumo zero resulta em total 0,00 sem detalhamento")
    void deveCalcularConsumoZero() {
        when(faixaRepository.findVigentesPorCategoria(Categoria.INDUSTRIAL)).thenReturn(faixasIndustrial());

        CalculoResponse r = service.calcular(new CalculoRequest(Categoria.INDUSTRIAL, 0));

        assertThat(r.valorTotal()).isEqualByComparingTo("0.00");
        assertThat(r.detalhamento()).isEmpty();
    }

    @Test
    @DisplayName("Consumo no limite da primeira faixa usa apenas a faixa 1")
    void deveCalcularNoLimite() {
        when(faixaRepository.findVigentesPorCategoria(Categoria.INDUSTRIAL)).thenReturn(faixasIndustrial());

        CalculoResponse r = service.calcular(new CalculoRequest(Categoria.INDUSTRIAL, 10));

        assertThat(r.valorTotal()).isEqualByComparingTo("10.00");
        assertThat(r.detalhamento()).hasSize(1);
    }

    @Test
    @DisplayName("Categoria sem faixas lança RegraNegocioException")
    void deveFalharSemFaixas() {
        when(faixaRepository.findVigentesPorCategoria(Categoria.PUBLICO)).thenReturn(List.of());

        assertThatThrownBy(() -> service.calcular(new CalculoRequest(Categoria.PUBLICO, 10)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Consumo fora da cobertura lança RegraNegocioException")
    void deveFalharForaDaCobertura() {
        when(faixaRepository.findVigentesPorCategoria(Categoria.INDUSTRIAL))
                .thenReturn(List.of(faixa(0, 10, "1.00"), faixa(11, 20, "2.00")));

        assertThatThrownBy(() -> service.calcular(new CalculoRequest(Categoria.INDUSTRIAL, 5000)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("excede");
    }
}
