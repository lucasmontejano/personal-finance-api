package com.lucas.financas.dto;

import com.lucas.financas.model.TipoTransacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CriarTransacaoDTO(

        @NotNull(message = "tipo é obrigatório")
        TipoTransacao tipo,

        @NotNull(message = "valor é obrigatório")
        @Positive(message = "valor tem que ser maior que zero")
        BigDecimal valor,

        @NotBlank(message = "descricao é obrigatória")
        @Size(max = 255)
        String descricao,

        @NotNull(message = "data é obrigatória")
        LocalDate data,

        @NotNull(message = "contaId é obrigatório")
        Long contaId,

        // só pra TRANSFERENCIA
        Long contaDestinoId,

        // null se for TRANSFERENCIA
        Long categoriaId,

        String observacoes
) {
}
