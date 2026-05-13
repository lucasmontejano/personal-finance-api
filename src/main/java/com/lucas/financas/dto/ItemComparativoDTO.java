package com.lucas.financas.dto;

import com.lucas.financas.model.StatusOrcamento;

import java.math.BigDecimal;

public record ItemComparativoDTO(
        Long orcamentoId,
        Long categoriaId,
        String categoriaNome,
        BigDecimal limite,
        BigDecimal gasto,
        BigDecimal restante,
        BigDecimal percentual,
        StatusOrcamento status
) {
}
