package com.lucas.financas.dto;

import com.lucas.financas.model.Orcamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrcamentoRespostaDTO(
        Long id,
        Long categoriaId,
        String categoriaNome,
        int ano,
        int mes,
        BigDecimal valorLimite,
        LocalDateTime criadoEm
) {

    public static OrcamentoRespostaDTO de(Orcamento o) {
        return new OrcamentoRespostaDTO(
                o.getId(),
                o.getCategoria().getId(),
                o.getCategoria().getNome(),
                o.getMesReferencia().getYear(),
                o.getMesReferencia().getMonthValue(),
                o.getValorLimite(),
                o.getCriadoEm()
        );
    }
}
