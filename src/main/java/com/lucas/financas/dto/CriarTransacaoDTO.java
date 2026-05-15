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

        // contaId XOR cartaoId pra RECEITA/DESPESA. obrigatorio em TRANSFERENCIA e PAGAMENTO_FATURA
        Long contaId,

        // só pra TRANSFERENCIA
        Long contaDestinoId,

        // null se for TRANSFERENCIA ou PAGAMENTO_FATURA
        Long categoriaId,

        // pra compra no cartao (XOR com contaId em RECEITA/DESPESA)
        Long cartaoId,

        // pra PAGAMENTO_FATURA
        Long faturaId,

        // 1 = a vista. >1 = parcelado em N. so faz sentido com cartaoId em DESPESA
        @Min(value = 1, message = "parcelas >= 1")
        @Max(value = 48, message = "parcelas <= 48")
        Integer parcelas,

        String observacoes
) {
}
