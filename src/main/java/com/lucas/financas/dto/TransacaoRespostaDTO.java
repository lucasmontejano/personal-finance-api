package com.lucas.financas.dto;

import com.lucas.financas.model.Conta;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
        LocalDateTime criadoEm
) {

    public static TransacaoRespostaDTO de(Transacao t) {
        Conta cd = t.getContaDestino();
        Categoria cat = t.getCategoria();
        return new TransacaoRespostaDTO(
                t.getId(),
                t.getTipo(),
                t.getValor(),
                t.getDescricao(),
                t.getData(),
                t.getObservacoes(),
                t.getConta().getId(),
                t.getConta().getNome(),
                cd != null ? cd.getId() : null,
                cd != null ? cd.getNome() : null,
                cat != null ? cat.getId() : null,
                cat != null ? cat.getNome() : null,
                t.getCriadoEm()
        );
    }
}
