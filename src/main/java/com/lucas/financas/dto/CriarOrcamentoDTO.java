package com.lucas.financas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CriarOrcamentoDTO(

        @NotNull(message = "categoriaId é obrigatório")
        Long categoriaId,

        @NotNull
        @Min(value = 2000, message = "ano fora do esperado")
        @Max(value = 2100, message = "ano fora do esperado")
        Integer ano,

        @NotNull
        @Min(value = 1, message = "mes vai de 1 a 12")
        @Max(value = 12, message = "mes vai de 1 a 12")
        Integer mes,

        @NotNull(message = "limite é obrigatório")
        @Positive(message = "limite tem que ser maior que zero")
        BigDecimal valorLimite
) {
}
