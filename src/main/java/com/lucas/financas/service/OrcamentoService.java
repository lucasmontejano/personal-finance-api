package com.lucas.financas.service;

import com.lucas.financas.dto.AtualizarOrcamentoDTO;
import com.lucas.financas.dto.ComparativoMesDTO;
import com.lucas.financas.dto.CriarOrcamentoDTO;
import com.lucas.financas.dto.ItemComparativoDTO;
import com.lucas.financas.dto.OrcamentoRespostaDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.Orcamento;
import com.lucas.financas.model.StatusOrcamento;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CategoriaRepository;
import com.lucas.financas.repository.GastoPorCategoria;
import com.lucas.financas.repository.OrcamentoRepository;
import com.lucas.financas.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrcamentoService {

    private static final BigDecimal LIMITE_ATENCAO = new BigDecimal("0.80");
    private static final BigDecimal CEM = new BigDecimal("100");

    private final OrcamentoRepository repository;
    private final CategoriaRepository categoriaRepository;
    private final TransacaoRepository transacaoRepository;

    public OrcamentoService(
            OrcamentoRepository repository,
            CategoriaRepository categoriaRepository,
            TransacaoRepository transacaoRepository
    ) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional
    public OrcamentoRespostaDTO criar(Usuario usuario, CriarOrcamentoDTO dto) {
        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(dto.categoriaId(), usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("categoria nao encontrada"));

        if (categoria.getTipo() != TipoCategoria.DESPESA) {
            throw new IllegalArgumentException("só dá pra orçar categoria de despesa");
        }

        LocalDate mesRef = LocalDate.of(dto.ano(), dto.mes(), 1);

        if (repository.existsByUsuarioIdAndCategoriaIdAndMesReferencia(usuario.getId(), categoria.getId(), mesRef)) {
            throw new IllegalArgumentException("ja existe um orçamento pra essa categoria nesse mes");
        }

        Orcamento o = new Orcamento(usuario, categoria, mesRef, dto.valorLimite());
        return OrcamentoRespostaDTO.de(repository.save(o));
    }

    @Transactional(readOnly = true)
    public List<OrcamentoRespostaDTO> listar(Usuario usuario, Integer ano, Integer mes) {
        List<Orcamento> orcamentos;
        if (ano != null && mes != null) {
            LocalDate mesRef = LocalDate.of(ano, mes, 1);
            orcamentos = repository.findByUsuarioIdAndMesReferenciaOrderByCategoriaNomeAsc(usuario.getId(), mesRef);
        } else {
            orcamentos = repository.findByUsuarioIdOrderByMesReferenciaDesc(usuario.getId());
        }
        return orcamentos.stream().map(OrcamentoRespostaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public OrcamentoRespostaDTO buscar(Usuario usuario, Long id) {
        return OrcamentoRespostaDTO.de(achar(usuario, id));
    }

    @Transactional
    public OrcamentoRespostaDTO atualizar(Usuario usuario, Long id, AtualizarOrcamentoDTO dto) {
        Orcamento o = achar(usuario, id);
        o.setValorLimite(dto.valorLimite());
        return OrcamentoRespostaDTO.de(o);
    }

    @Transactional
    public void deletar(Usuario usuario, Long id) {
        Orcamento o = achar(usuario, id);
        repository.delete(o);
    }

    @Transactional(readOnly = true)
    public ComparativoMesDTO comparativo(Usuario usuario, int ano, int mes) {
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        List<Orcamento> orcamentos = repository
                .findByUsuarioIdAndMesReferenciaOrderByCategoriaNomeAsc(usuario.getId(), inicio);

        // pega o total gasto por categoria nesse mes (uma query só pra todas)
        Map<Long, BigDecimal> gastoPorCategoria = new HashMap<>();
        for (GastoPorCategoria g : transacaoRepository.agregaPorCategoriaETipo(usuario.getId(), TipoTransacao.DESPESA, inicio, fim)) {
            gastoPorCategoria.put(g.getCategoriaId(), g.getTotal());
        }

        BigDecimal totalOrcado = BigDecimal.ZERO;
        BigDecimal totalGasto = BigDecimal.ZERO;
        List<ItemComparativoDTO> itens = new java.util.ArrayList<>();

        for (Orcamento o : orcamentos) {
            BigDecimal limite = o.getValorLimite();
            BigDecimal gasto = gastoPorCategoria.getOrDefault(o.getCategoria().getId(), BigDecimal.ZERO);
            BigDecimal restante = limite.subtract(gasto);
            BigDecimal percentual = gasto.multiply(CEM).divide(limite, 2, RoundingMode.HALF_UP);

            StatusOrcamento status;
            if (gasto.compareTo(limite) > 0) {
                status = StatusOrcamento.ESTOUROU;
            } else if (gasto.compareTo(limite.multiply(LIMITE_ATENCAO)) >= 0) {
                status = StatusOrcamento.ATENCAO;
            } else {
                status = StatusOrcamento.DENTRO;
            }

            itens.add(new ItemComparativoDTO(
                    o.getId(),
                    o.getCategoria().getId(),
                    o.getCategoria().getNome(),
                    limite,
                    gasto,
                    restante,
                    percentual,
                    status
            ));

            totalOrcado = totalOrcado.add(limite);
            totalGasto = totalGasto.add(gasto);
        }

        return new ComparativoMesDTO(ano, mes, totalOrcado, totalGasto, itens);
    }

    private Orcamento achar(Usuario usuario, Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("orçamento nao encontrado"));
    }
}
