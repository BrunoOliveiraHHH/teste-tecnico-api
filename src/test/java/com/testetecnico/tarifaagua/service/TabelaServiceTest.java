package com.testetecnico.tarifaagua.service;

import com.testetecnico.tarifaagua.domain.Categoria;
import com.testetecnico.tarifaagua.domain.TabelaTarifaria;
import com.testetecnico.tarifaagua.dto.CategoriaFaixasRequest;
import com.testetecnico.tarifaagua.dto.CriarTabelaRequest;
import com.testetecnico.tarifaagua.dto.FaixaRequest;
import com.testetecnico.tarifaagua.dto.TabelaResponse;
import com.testetecnico.tarifaagua.exception.RecursoNaoEncontradoException;
import com.testetecnico.tarifaagua.exception.RegraNegocioException;
import com.testetecnico.tarifaagua.repository.TabelaTarifariaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TabelaService - validação de faixas e soft delete")
class TabelaServiceTest {

    @Mock
    private TabelaTarifariaRepository repository;

    @InjectMocks
    private TabelaService service;

    private FaixaRequest faixa(int inicio, int fim, String valor) {
        return new FaixaRequest(inicio, fim, new BigDecimal(valor));
    }

    private CriarTabelaRequest tabelaCom(List<FaixaRequest> faixas) {
        return new CriarTabelaRequest("Tabela", LocalDate.of(2026, 1, 1),
                List.of(new CategoriaFaixasRequest(Categoria.INDUSTRIAL, faixas)));
    }

    @Test
    @DisplayName("Cria tabela válida e persiste")
    void deveCriarTabelaValida() {
        when(repository.save(any(TabelaTarifaria.class))).thenAnswer(inv -> {
            TabelaTarifaria t = inv.getArgument(0);
            t.setId(10L);
            return t;
        });

        TabelaResponse r = service.criar(tabelaCom(List.of(faixa(0, 10, "1.00"), faixa(11, 20, "2.00"))));

        assertThat(r.id()).isEqualTo(10L);
        assertThat(r.ativo()).isTrue();
        verify(repository).save(any(TabelaTarifaria.class));
    }

    @Test
    @DisplayName("Rejeita faixas que não iniciam em 0")
    void deveRejeitarSemInicioEmZero() {
        assertThatThrownBy(() -> service.criar(tabelaCom(List.of(faixa(1, 10, "1.00")))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("iniciar em 0");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Rejeita início maior ou igual ao fim")
    void deveRejeitarOrdemInvalida() {
        assertThatThrownBy(() -> service.criar(tabelaCom(List.of(faixa(0, 10, "1.00"), faixa(15, 12, "2.00")))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("menor que o fim");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Rejeita faixas sobrepostas")
    void deveRejeitarSobreposicao() {
        assertThatThrownBy(() -> service.criar(tabelaCom(List.of(faixa(0, 10, "1.00"), faixa(8, 20, "2.00")))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("sobrep");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Rejeita lacuna entre faixas")
    void deveRejeitarLacuna() {
        assertThatThrownBy(() -> service.criar(tabelaCom(List.of(faixa(0, 10, "1.00"), faixa(15, 20, "2.00")))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("lacuna");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Excluir realiza soft delete (ativo = false)")
    void deveFazerSoftDelete() {
        TabelaTarifaria tabela = new TabelaTarifaria();
        tabela.setId(5L);
        tabela.setAtivo(true);
        when(repository.findById(5L)).thenReturn(Optional.of(tabela));

        service.excluir(5L);

        assertThat(tabela.isAtivo()).isFalse();
        verify(repository).save(tabela);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Excluir id inexistente lança RecursoNaoEncontradoException")
    void deveFalharAoExcluirInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
