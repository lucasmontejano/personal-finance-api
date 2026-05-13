package com.lucas.financas.dto;

import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.Conta;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.TransacaoRecorrente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransacaoRecorrenteRespostaDTO(
        Long id,
        TipoTransacao tipo,
        BigDecimal valor,
        String descricao,
        Long contaId,
        String contaNome,
        Long contaDestinoId,
        String contaDestinoNome,
        Long categoriaId,
        String categoriaNome,
        Short diaDoMes,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean ativa,
        LocalDate ultimaExecucao,
        LocalDateTime criadoEm
) {

    public static TransacaoRecorrenteRespostaDTO de(TransacaoRecorrente r) {
        Conta cd = r.getContaDestino();
        Categoria cat = r.getCategoria();
        return new TransacaoRecorrenteRespostaDTO(
                r.getId(),
                r.getTipo(),
                r.getValor(),
                r.getDescricao(),
                r.getConta().getId(),
                r.getConta().getNome(),
                cd != null ? cd.getId() : null,
                cd != null ? cd.getNome() : null,
                cat != null ? cat.getId() : null,
                cat != null ? cat.getNome() : null,
                r.getDiaDoMes(),
                r.getDataInicio(),
                r.getDataFim(),
                r.isAtiva(),
                r.getUltimaExecucao(),
                r.getCriadoEm()
        );
    }
}
