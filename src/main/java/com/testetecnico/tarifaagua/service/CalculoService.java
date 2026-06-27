package com.testetecnico.tarifaagua.service;

import com.testetecnico.tarifaagua.domain.Categoria;
import com.testetecnico.tarifaagua.domain.FaixaConsumo;
import com.testetecnico.tarifaagua.dto.CalculoRequest;
import com.testetecnico.tarifaagua.dto.CalculoResponse;
import com.testetecnico.tarifaagua.exception.RegraNegocioException;
import com.testetecnico.tarifaagua.repository.FaixaConsumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Calcula o valor a pagar de forma progressiva por faixas, usando exclusivamente
 * os valores parametrizados no banco (parametrização total).
 *
 * <p>Exemplo (Industrial, 18 m³): faixa 0–10 → 10 × R$ 1,00 = R$ 10,00; faixa
 * 11–20 → 8 × R$ 2,00 = R$ 16,00; total = R$ 26,00.</p>
 */
@Service
public class CalculoService {

    private final FaixaConsumoRepository faixaRepository;

    public CalculoService(FaixaConsumoRepository faixaRepository) {
        this.faixaRepository = faixaRepository;
    }

    @Transactional(readOnly = true)
    public CalculoResponse calcular(CalculoRequest req) {
        Categoria categoria = req.categoria();
        int consumo = req.consumo();

        List<FaixaConsumo> faixas = obterFaixasVigentes(categoria);

        int coberturaMaxima = faixas.get(faixas.size() - 1).getFim();
        if (consumo > coberturaMaxima) {
            throw new RegraNegocioException("O consumo de " + consumo
                    + " m³ excede a cobertura máxima das faixas (" + coberturaMaxima + " m³).");
        }

        List<CalculoResponse.DetalhamentoItem> detalhamento = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;
        int pisoAnterior = 0;

        for (FaixaConsumo faixa : faixas) {
            int teto = Math.min(consumo, faixa.getFim());
            int m3Cobrados = teto - pisoAnterior;

            if (m3Cobrados > 0) {
                BigDecimal subtotal = faixa.getValorUnitario().multiply(BigDecimal.valueOf(m3Cobrados));
                detalhamento.add(new CalculoResponse.DetalhamentoItem(
                        new CalculoResponse.FaixaIntervalo(faixa.getInicio(), faixa.getFim()),
                        m3Cobrados, faixa.getValorUnitario(), subtotal));
                valorTotal = valorTotal.add(subtotal);
            }

            pisoAnterior = faixa.getFim();
            if (consumo <= faixa.getFim()) {
                break;
            }
        }

        valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);
        return new CalculoResponse(categoria, consumo, valorTotal, detalhamento);
    }

    private List<FaixaConsumo> obterFaixasVigentes(Categoria categoria) {
        List<FaixaConsumo> vigentes = faixaRepository.findVigentesPorCategoria(categoria);
        if (vigentes.isEmpty()) {
            throw new RegraNegocioException(
                    "Não há tabela tarifária ativa com faixas para a categoria " + categoria + ".");
        }
        Long idTabela = vigentes.get(0).getTabela().getId();
        return vigentes.stream()
                .filter(f -> idTabela.equals(f.getTabela().getId()))
                .sorted(Comparator.comparingInt(FaixaConsumo::getInicio))
                .toList();
    }
}
