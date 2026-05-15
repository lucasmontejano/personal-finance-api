package com.lucas.financas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagarFaturaDTO(

        @NotNull(message = "contaId é obrigatório")
        Long contaId,

        @NotNull(message = "valor é obrigatório")
        @Positive(message = "valor tem que ser maior que zero")
        BigDecimal valor,

        // opcional. se null, usa hoje
        LocalDate data
) {
}
