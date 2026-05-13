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

public record CriarTransacaoRecorrenteDTO(

        @NotNull(message = "tipo é obrigatório")
        TipoTransacao tipo,

        @NotNull(message = "valor é obrigatório")
        @Positive(message = "valor tem que ser maior que zero")
        BigDecimal valor,

        @NotBlank(message = "descricao é obrigatória")
        @Size(max = 255)
        String descricao,

        @NotNull(message = "contaId é obrigatório")
        Long contaId,

        Long contaDestinoId,
        Long categoriaId,

        @NotNull(message = "diaDoMes é obrigatório")
        @Min(value = 1, message = "diaDoMes tem que ser entre 1 e 31")
        @Max(value = 31, message = "diaDoMes tem que ser entre 1 e 31")
        Short diaDoMes,

        @NotNull(message = "dataInicio é obrigatória")
        LocalDate dataInicio,

        // se vazio, recorrencia roda pra sempre
        LocalDate dataFim
) {
}
