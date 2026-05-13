package com.lucas.financas.dto;

import com.lucas.financas.model.TipoCategoria;

import java.math.BigDecimal;

public record TotalCategoriaDTO(
        Long categoriaId,
        String categoriaNome,
        TipoCategoria tipo,
        BigDecimal total,
        BigDecimal percentual
) {
}
