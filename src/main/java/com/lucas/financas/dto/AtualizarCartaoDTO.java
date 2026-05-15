package com.lucas.financas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// pode editar nome, limite, dias, bandeira e cor. nao deixo trocar a conta padrao
// (cria de novo se precisar) pra nao complicar
public record AtualizarCartaoDTO(

        @NotBlank
        @Size(max = 120)
        String nome,

        @Size(max = 30)
        String bandeira,

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal limite,

        @NotNull
        @Min(1) @Max(31)
        Integer diaFechamento,

        @NotNull
        @Min(1) @Max(31)
        Integer diaVencimento,

        Long contaPadraoPagamentoId,

        @Size(max = 7)
        String cor
) {
}
