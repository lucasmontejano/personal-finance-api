package com.lucas.financas.dto;

import com.lucas.financas.model.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarContaDTO(

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120)
        String nome,

        @NotNull(message = "tipo é obrigatório")
        TipoConta tipo,

        @NotNull(message = "saldo inicial é obrigatório")
        BigDecimal saldoInicial,

        @Size(max = 7)
        String cor
) {
}
