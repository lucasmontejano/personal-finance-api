package com.lucas.financas.dto;

import java.math.BigDecimal;
import java.util.List;

public record ComparativoMesDTO(
        int ano,
        int mes,
        BigDecimal totalOrcado,
        BigDecimal totalGasto,
        List<ItemComparativoDTO> itens
) {
}
