package com.lucas.financas.dto;

import com.lucas.financas.model.Cartao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CartaoRespostaDTO(
        Long id,
        String nome,
        String bandeira,
        BigDecimal limite,
        BigDecimal limiteUsado,
        BigDecimal limiteDisponivel,
        Integer diaFechamento,
        Integer diaVencimento,
        Long contaPadraoPagamentoId,
        String cor,
        boolean ativo,
        LocalDateTime criadoEm
) {

    public static CartaoRespostaDTO de(Cartao c, BigDecimal limiteUsado) {
        BigDecimal disponivel = c.getLimite().subtract(limiteUsado);
        Long contaPadraoId = c.getContaPadraoPagamento() != null
                ? c.getContaPadraoPagamento().getId()
                : null;
        return new CartaoRespostaDTO(
                c.getId(),
                c.getNome(),
                c.getBandeira(),
                c.getLimite(),
                limiteUsado,
                disponivel,
                c.getDiaFechamento(),
                c.getDiaVencimento(),
                contaPadraoId,
                c.getCor(),
                c.isAtivo(),
                c.getCriadoEm()
        );
    }
}
