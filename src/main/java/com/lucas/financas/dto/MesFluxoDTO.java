package com.lucas.financas.dto;

import java.math.BigDecimal;

public record MesFluxoDTO(
        int ano,
        int mes,
        BigDecimal receitas,
        BigDecimal despesas,
        BigDecimal saldo
) {
}
