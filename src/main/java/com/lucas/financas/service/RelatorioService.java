package com.lucas.financas.service;

import com.lucas.financas.dto.FluxoCaixaDTO;
import com.lucas.financas.dto.MesFluxoDTO;
import com.lucas.financas.dto.RelatorioPorCategoriaDTO;
import com.lucas.financas.dto.ResumoMensalDTO;
import com.lucas.financas.dto.TotalCategoriaDTO;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Transacao;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CategoriaRepository;
import com.lucas.financas.repository.GastoPorCategoria;
import com.lucas.financas.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final TransacaoRepository transacaoRepository;
    private final CategoriaRepository categoriaRepository;

    public RelatorioService(TransacaoRepository transacaoRepository, CategoriaRepository categoriaRepository) {
        this.transacaoRepository = transacaoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public ResumoMensalDTO resumoMensal(Usuario user, int ano, int mes) {
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        BigDecimal receitas = transacaoRepository.somarPorUsuarioTipoEPeriodo(user.getId(), TipoTransacao.RECEITA, inicio, fim);
        BigDecimal despesas = transacaoRepository.somarPorUsuarioTipoEPeriodo(user.getId(), TipoTransacao.DESPESA, inicio, fim);
        long qtdR = transacaoRepository.countByUsuarioIdAndTipoAndDataBetweenAndDeletadoFalse(user.getId(), TipoTransacao.RECEITA, inicio, fim);
        long qtdD = transacaoRepository.countByUsuarioIdAndTipoAndDataBetweenAndDeletadoFalse(user.getId(), TipoTransacao.DESPESA, inicio, fim);

        return new ResumoMensalDTO(ano, mes, receitas, despesas, receitas.subtract(despesas), qtdR, qtdD);
    }

    @Transactional(readOnly = true)
    public RelatorioPorCategoriaDTO porCategoria(Usuario user, LocalDate inicio, LocalDate fim, TipoCategoria tipoCategoria) {
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("fim nao pode ser antes de inicio");
        }

        TipoTransacao tipoTx = (tipoCategoria == TipoCategoria.RECEITA)
                ? TipoTransacao.RECEITA
                : TipoTransacao.DESPESA;

        List<GastoPorCategoria> agregado = transacaoRepository.agregaPorCategoriaETipo(user.getId(), tipoTx, inicio, fim);

        // pega os nomes das categorias num map pra nao fazer N queries
        Map<Long, String> nomes = new HashMap<>();
        for (Categoria c : categoriaRepository.findByUsuarioIdOrderByTipoAscNomeAsc(user.getId())) {
            nomes.put(c.getId(), c.getNome());
        }

        BigDecimal total = agregado.stream()
                .map(GastoPorCategoria::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TotalCategoriaDTO> itens = agregado.stream()
                .map(g -> {
                    BigDecimal pct = total.compareTo(BigDecimal.ZERO) > 0
                            ? g.getTotal().multiply(CEM).divide(total, 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return new TotalCategoriaDTO(
                            g.getCategoriaId(),
                            nomes.getOrDefault(g.getCategoriaId(), "(removida)"),
                            tipoCategoria,
                            g.getTotal(),
                            pct
                    );
                })
                .sorted(Comparator.comparing(TotalCategoriaDTO::total).reversed())
                .toList();

        return new RelatorioPorCategoriaDTO(inicio, fim, tipoCategoria, total, itens);
    }

    @Transactional(readOnly = true)
    public FluxoCaixaDTO fluxoCaixa(Usuario user, int meses) {
        if (meses < 1 || meses > 36) {
            throw new IllegalArgumentException("meses precisa ser entre 1 e 36");
        }

        YearMonth atual = YearMonth.now();
        YearMonth inicial = atual.minusMonths(meses - 1L);
        LocalDate dataInicio = inicial.atDay(1);
        LocalDate dataFim = atual.atEndOfMonth();

        List<Transacao> transacoes = transacaoRepository.buscarPorPeriodoETipos(
                user.getId(),
                dataInicio,
                dataFim,
                List.of(TipoTransacao.RECEITA, TipoTransacao.DESPESA)
        );

        Map<YearMonth, BigDecimal> receitasPorMes = new HashMap<>();
        Map<YearMonth, BigDecimal> despesasPorMes = new HashMap<>();

        for (Transacao t : transacoes) {
            YearMonth ym = YearMonth.from(t.getData());
            if (t.getTipo() == TipoTransacao.RECEITA) {
                receitasPorMes.merge(ym, t.getValor(), BigDecimal::add);
            } else {
                despesasPorMes.merge(ym, t.getValor(), BigDecimal::add);
            }
        }

        // gera 1 entrada por mes do periodo, mesmo se nao tiver transação
        List<MesFluxoDTO> mesesDTO = new ArrayList<>();
        YearMonth cursor = inicial;
        while (!cursor.isAfter(atual)) {
            BigDecimal r = receitasPorMes.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal d = despesasPorMes.getOrDefault(cursor, BigDecimal.ZERO);
            mesesDTO.add(new MesFluxoDTO(cursor.getYear(), cursor.getMonthValue(), r, d, r.subtract(d)));
            cursor = cursor.plusMonths(1);
        }

        return new FluxoCaixaDTO(meses, mesesDTO);
    }
}
