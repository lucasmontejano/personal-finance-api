package com.lucas.financas.dto;

import com.lucas.financas.model.Cartao;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.Conta;
import com.lucas.financas.model.Fatura;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoRespostaDTO(
        Long id,
        TipoTransacao tipo,
        BigDecimal valor,
        String descricao,
        LocalDate data,
        String observacoes,
        Long contaId,
        String contaNome,
        Long contaDestinoId,
        String contaDestinoNome,
        Long categoriaId,
        String categoriaNome,
        Long cartaoId,
        String cartaoNome,
        Long faturaId,
        UUID compraParceladaId,
        Integer numeroParcela,
        Integer totalParcelas,
        LocalDateTime criadoEm
) {

    public static TransacaoRespostaDTO de(Transacao t) {
        Conta c = t.getConta();
        Conta cd = t.getContaDestino();
        Categoria cat = t.getCategoria();
        Cartao ca = t.getCartao();
        Fatura f = t.getFatura();
        return new TransacaoRespostaDTO(
                t.getId(),
                t.getTipo(),
                t.getValor(),
                t.getDescricao(),
                t.getData(),
                t.getObservacoes(),
                c != null ? c.getId() : null,
                c != null ? c.getNome() : null,
                cd != null ? cd.getId() : null,
                cd != null ? cd.getNome() : null,
                cat != null ? cat.getId() : null,
                cat != null ? cat.getNome() : null,
                ca != null ? ca.getId() : null,
                ca != null ? ca.getNome() : null,
                f != null ? f.getId() : null,
                t.getCompraParceladaId(),
                t.getNumeroParcela(),
                t.getTotalParcelas(),
                t.getCriadoEm()
        );
    }
}
