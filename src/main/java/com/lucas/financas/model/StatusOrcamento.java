package com.lucas.financas.model;

public enum StatusOrcamento {
    DENTRO,    // gasto até 80% do limite
    ATENCAO,   // gasto entre 80% e 100%
    ESTOUROU   // passou do limite
}
