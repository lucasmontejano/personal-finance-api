package com.lucas.financas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarCartaoDTO(

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120)
        String nome,

        @Size(max = 30)
        String bandeira,

        @NotNull(message = "limite é obrigatório")
        @DecimalMin(value = "0.00", message = "limite não pode ser negativo")
        BigDecimal limite,

        @NotNull(message = "dia de fechamento é obrigatório")
        @Min(value = 1, message = "dia de fechamento entre 1 e 31")
        @Max(value = 31, message = "dia de fechamento entre 1 e 31")
        Integer diaFechamento,

        @NotNull(message = "dia de vencimento é obrigatório")
        @Min(value = 1, message = "dia de vencimento entre 1 e 31")
        @Max(value = 31, message = "dia de vencimento entre 1 e 31")
        Integer diaVencimento,

        Long contaPadraoPagamentoId,

        @Size(max = 7)
        String cor
) {
}
