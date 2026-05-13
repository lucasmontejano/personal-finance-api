package com.lucas.financas.dto;

import com.lucas.financas.model.TipoTransacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AtualizarTransacaoRecorrenteDTO(

        @NotNull TipoTransacao tipo,

        @NotNull @Positive BigDecimal valor,

        @NotBlank @Size(max = 255) String descricao,

        @NotNull Long contaId,
        Long contaDestinoId,
        Long categoriaId,

        @NotNull @Min(1) @Max(31) Short diaDoMes,

        @NotNull LocalDate dataInicio,
        LocalDate dataFim,

        // pra pausar/retomar sem deletar
        @NotNull Boolean ativa
) {
}
