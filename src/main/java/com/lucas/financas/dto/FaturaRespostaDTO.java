package com.lucas.financas.dto;

import com.lucas.financas.model.Fatura;
import com.lucas.financas.model.StatusFatura;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaturaRespostaDTO(
        Long id,
        Long cartaoId,
        LocalDate mesReferencia,
        LocalDate dataFechamento,
        LocalDate dataVencimento,
        StatusFatura status,
        BigDecimal valorTotal,
        BigDecimal valorPago,
        BigDecimal valorPendente
) {

    public static FaturaRespostaDTO de(Fatura f, BigDecimal valorTotal) {
        BigDecimal pendente = valorTotal.subtract(f.getValorPago());
        return new FaturaRespostaDTO(
                f.getId(),
                f.getCartao().getId(),
                f.getMesReferencia(),
                f.getDataFechamento(),
                f.getDataVencimento(),
                f.getStatus(),
                valorTotal,
                f.getValorPago(),
                pendente
        );
    }
}
