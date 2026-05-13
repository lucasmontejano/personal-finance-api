package com.lucas.financas.dto;

import com.lucas.financas.model.Conta;
import com.lucas.financas.model.TipoConta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContaRespostaDTO(
        Long id,
        String nome,
        TipoConta tipo,
        BigDecimal saldoInicial,
        BigDecimal saldo,
        String cor,
        boolean ativa,
        LocalDateTime criadoEm
) {

    public static ContaRespostaDTO de(Conta c, BigDecimal saldo) {
        return new ContaRespostaDTO(
                c.getId(),
                c.getNome(),
                c.getTipo(),
                c.getSaldoInicial(),
                saldo,
                c.getCor(),
                c.isAtiva(),
                c.getCriadoEm()
        );
    }
}
