package com.lucas.financas.dto;

import java.math.BigDecimal;

public record ResumoMensalDTO(
        int ano,
        int mes,
        BigDecimal totalReceitas,
        BigDecimal totalDespesas,
        BigDecimal saldoMes,
        long quantidadeReceitas,
        long quantidadeDespesas
) {
}
