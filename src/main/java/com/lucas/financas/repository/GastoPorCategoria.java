package com.lucas.financas.repository;

import java.math.BigDecimal;

// projection do Spring Data - usada na query de agregação
public interface GastoPorCategoria {
    Long getCategoriaId();
    BigDecimal getTotal();
}
