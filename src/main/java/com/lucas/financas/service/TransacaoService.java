package com.lucas.financas.service;

import com.lucas.financas.dto.AtualizarTransacaoDTO;
import com.lucas.financas.dto.CriarTransacaoDTO;
import com.lucas.financas.dto.PaginaDTO;
import com.lucas.financas.dto.TransacaoRespostaDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.Conta;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Transacao;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CategoriaRepository;
import com.lucas.financas.repository.ContaRepository;
import com.lucas.financas.repository.TransacaoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;

    public TransacaoService(
            TransacaoRepository repository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.repository = repository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional
    public TransacaoRespostaDTO criar(Usuario user, String idempotencyKey, CriarTransacaoDTO dto) {
        // se mandou idempotency key e ja existe, devolve a mesma sem criar de novo
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existente = repository.findByUsuarioIdAndIdempotencyKey(user.getId(), idempotencyKey);
            if (existente.isPresent()) {
                return TransacaoRespostaDTO.de(existente.get());
            }
        }

        Refs refs = validar(user, dto.tipo(), dto.contaId(), dto.contaDestinoId(), dto.categoriaId());

        Transacao t = new Transacao(
                user,
                refs.conta(),
                refs.contaDestino(),
                refs.categoria(),
                dto.tipo(),
                dto.valor(),
                dto.descricao(),
                dto.data(),
                dto.observacoes()
        );
        t.setIdempotencyKey(idempotencyKey != null && idempotencyKey.isBlank() ? null : idempotencyKey);

        return TransacaoRespostaDTO.de(repository.save(t));
    }

    @Transactional(readOnly = true)
    public PaginaDTO<TransacaoRespostaDTO> listar(
            Usuario user,
            Long contaId,
            Long categoriaId,
            TipoTransacao tipo,
            LocalDate inicio,
            LocalDate fim,
            Pageable pageable
    ) {
        var pagina = repository.buscar(user.getId(), contaId, categoriaId, tipo, inicio, fim, pageable)
                .map(TransacaoRespostaDTO::de);
        return PaginaDTO.de(pagina);
    }

    @Transactional(readOnly = true)
    public TransacaoRespostaDTO buscar(Usuario user, Long id) {
        return TransacaoRespostaDTO.de(achar(user, id));
    }

    @Transactional
    public TransacaoRespostaDTO atualizar(Usuario user, Long id, AtualizarTransacaoDTO dto) {
        Transacao t = achar(user, id);
        Refs refs = validar(user, dto.tipo(), dto.contaId(), dto.contaDestinoId(), dto.categoriaId());

        t.setTipo(dto.tipo());
        t.setValor(dto.valor());
        t.setDescricao(dto.descricao());
        t.setData(dto.data());
        t.setObservacoes(dto.observacoes());
        t.setConta(refs.conta());
        t.setContaDestino(refs.contaDestino());
        t.setCategoria(refs.categoria());

        return TransacaoRespostaDTO.de(t);
    }

    @Transactional
    public void deletar(Usuario user, Long id) {
        // soft delete - mantem o historico
        Transacao t = achar(user, id);
        t.setDeletado(true);
    }

    private Transacao achar(Usuario user, Long id) {
        return repository.findByIdAndUsuarioIdAndDeletadoFalse(id, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("transação nao encontrada"));
    }

    // valida tipo + carrega conta, contaDestino e categoria conforme as regras
    private Refs validar(Usuario user, TipoTransacao tipo, Long contaId, Long contaDestinoId, Long categoriaId) {
        Conta conta = contaRepository.findByIdAndUsuarioId(contaId, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("conta nao encontrada"));

        Conta contaDestino = null;
        Categoria categoria = null;

        switch (tipo) {
            case RECEITA, DESPESA -> {
                if (contaDestinoId != null) {
                    throw new IllegalArgumentException("contaDestino só é permitida em transferencia");
                }
                if (categoriaId == null) {
                    throw new IllegalArgumentException("categoria é obrigatória pra receita/despesa");
                }
                categoria = categoriaRepository.findByIdAndUsuarioId(categoriaId, user.getId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("categoria nao encontrada"));

                TipoCategoria esperado = (tipo == TipoTransacao.RECEITA)
                        ? TipoCategoria.RECEITA
                        : TipoCategoria.DESPESA;
                if (categoria.getTipo() != esperado) {
                    throw new IllegalArgumentException("categoria nao bate com o tipo da transação");
                }
            }
            case TRANSFERENCIA -> {
                if (contaDestinoId == null) {
                    throw new IllegalArgumentException("contaDestino é obrigatória pra transferencia");
                }
                if (contaDestinoId.equals(contaId)) {
                    throw new IllegalArgumentException("conta origem e destino nao podem ser iguais");
                }
                if (categoriaId != null) {
                    throw new IllegalArgumentException("transferencia nao tem categoria");
                }
                contaDestino = contaRepository.findByIdAndUsuarioId(contaDestinoId, user.getId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("conta destino nao encontrada"));
            }
        }

        return new Refs(conta, contaDestino, categoria);
    }

    private record Refs(Conta conta, Conta contaDestino, Categoria categoria) {
    }
}
