package com.lucas.financas.dto;

import com.lucas.financas.model.TipoCategoria;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RelatorioPorCategoriaDTO(
        LocalDate inicio,
        LocalDate fim,
        TipoCategoria tipo,
        BigDecimal total,
        List<TotalCategoriaDTO> itens
) {
}
