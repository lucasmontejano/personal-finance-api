package com.lucas.financas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// só deixo mudar o limite. pra trocar mes/categoria, apaga e cria de novo
public record AtualizarOrcamentoDTO(

        @NotNull
        @Positive(message = "limite tem que ser maior que zero")
        BigDecimal valorLimite
) {
}
