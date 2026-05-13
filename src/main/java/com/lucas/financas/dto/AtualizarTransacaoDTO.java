package com.lucas.financas.dto;

import com.lucas.financas.model.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AtualizarTransacaoDTO(

        @NotNull TipoTransacao tipo,

        @NotNull @Positive BigDecimal valor,

        @NotBlank @Size(max = 255) String descricao,

        @NotNull LocalDate data,

        @NotNull Long contaId,

        Long contaDestinoId,
        Long categoriaId,
        String observacoes
) {
}
