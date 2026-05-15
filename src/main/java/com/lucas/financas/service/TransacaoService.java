package com.lucas.financas.service;

import com.lucas.financas.dto.AtualizarTransacaoDTO;
import com.lucas.financas.dto.CriarTransacaoDTO;
import com.lucas.financas.dto.PaginaDTO;
import com.lucas.financas.dto.TransacaoRespostaDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Cartao;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.Conta;
import com.lucas.financas.model.Fatura;
import com.lucas.financas.model.StatusFatura;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Transacao;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CartaoRepository;
import com.lucas.financas.repository.CategoriaRepository;
import com.lucas.financas.repository.ContaRepository;
import com.lucas.financas.repository.FaturaRepository;
import com.lucas.financas.repository.TransacaoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class TransacaoService {

    private final TransacaoRepository repository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final CartaoRepository cartaoRepository;
    private final FaturaRepository faturaRepository;
    private final FaturaService faturaService;

    public TransacaoService(
            TransacaoRepository repository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository,
            CartaoRepository cartaoRepository,
            FaturaRepository faturaRepository,
            FaturaService faturaService
    ) {
        this.repository = repository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.cartaoRepository = cartaoRepository;
        this.faturaRepository = faturaRepository;
        this.faturaService = faturaService;
    }

    @Transactional
    public TransacaoRespostaDTO criar(Usuario user, String idempotencyKey, CriarTransacaoDTO dto) {
        // idempotency: se ja existe com essa key, devolve sem criar
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existente = repository.findByUsuarioIdAndIdempotencyKey(user.getId(), idempotencyKey);
            if (existente.isPresent()) {
                return TransacaoRespostaDTO.de(existente.get());
            }
        }

        Refs refs = validar(user, dto);

        int parcelas = dto.parcelas() != null ? dto.parcelas() : 1;

        if (parcelas > 1) {
            if (refs.cartao() == null || dto.tipo() != TipoTransacao.DESPESA) {
                throw new IllegalArgumentException("parcelamento so com cartao e DESPESA");
            }
            return criarParcelado(user, idempotencyKey, dto, refs, parcelas);
        }

        return criarSimples(user, idempotencyKey, dto, refs);
    }

    private TransacaoRespostaDTO criarSimples(Usuario user, String idempotencyKey, CriarTransacaoDTO dto, Refs refs) {
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
        if (refs.cartao() != null) {
            t.setCartao(refs.cartao());
            t.setFatura(faturaService.findOuCriaPorData(refs.cartao(), dto.data()));
        }
        if (refs.fatura() != null) {
            // pagamento de fatura: amarra na fatura e atualiza valor_pago
            t.setFatura(refs.fatura());
            aplicarPagamentoNaFatura(refs.fatura(), dto.valor());
        }
        t.setIdempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null);
        return TransacaoRespostaDTO.de(repository.save(t));
    }

    private TransacaoRespostaDTO criarParcelado(
            Usuario user, String idempotencyKey, CriarTransacaoDTO dto, Refs refs, int parcelas
    ) {
        UUID compraId = UUID.randomUUID();
        BigDecimal total = dto.valor();
        BigDecimal porParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);
        // primeira parcela absorve a sobra de arredondamento pro total bater
        BigDecimal sobra = total.subtract(porParcela.multiply(BigDecimal.valueOf(parcelas)));

        Transacao primeira = null;
        LocalDate dataParcela = dto.data();
        for (int i = 1; i <= parcelas; i++) {
            BigDecimal v = (i == 1) ? porParcela.add(sobra) : porParcela;
            String desc = dto.descricao() + " (" + i + "/" + parcelas + ")";

            Transacao t = new Transacao(
                    user, null, null, refs.categoria(),
                    dto.tipo(), v, desc, dataParcela, dto.observacoes()
            );
            t.setCartao(refs.cartao());
            t.setFatura(faturaService.findOuCriaPorData(refs.cartao(), dataParcela));
            t.setCompraParceladaId(compraId);
            t.setNumeroParcela(i);
            t.setTotalParcelas(parcelas);

            if (i == 1 && idempotencyKey != null && !idempotencyKey.isBlank()) {
                t.setIdempotencyKey(idempotencyKey);
            }

            Transacao saved = repository.save(t);
            if (primeira == null) primeira = saved;

            dataParcela = dataParcela.plusMonths(1);
        }
        return TransacaoRespostaDTO.de(primeira);
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

        // pagamento de fatura nao pode ser editado — delete e refaz
        if (t.getTipo() == TipoTransacao.PAGAMENTO_FATURA) {
            throw new IllegalArgumentException("pagamento de fatura nao pode ser editado");
        }

        // transacao no cartao: regras restritas (so muda valor, descricao, categoria, observacoes)
        if (t.getCartao() != null) {
            if (!t.getData().equals(dto.data())) {
                throw new IllegalArgumentException("nao pode mudar a data de transacao no cartao");
            }
            if (t.getTipo() != dto.tipo()) {
                throw new IllegalArgumentException("nao pode mudar o tipo de transacao no cartao");
            }
            if (dto.contaId() != null) {
                throw new IllegalArgumentException("transacao no cartao nao tem contaId");
            }
            if (dto.contaDestinoId() != null) {
                throw new IllegalArgumentException("transacao no cartao nao tem contaDestino");
            }
            if (dto.categoriaId() == null) {
                throw new IllegalArgumentException("categoria obrigatoria");
            }
            Categoria cat = categoriaRepository.findByIdAndUsuarioId(dto.categoriaId(), user.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("categoria nao encontrada"));
            TipoCategoria esperado = (t.getTipo() == TipoTransacao.RECEITA)
                    ? TipoCategoria.RECEITA : TipoCategoria.DESPESA;
            if (cat.getTipo() != esperado) {
                throw new IllegalArgumentException("categoria nao bate com o tipo da transação");
            }
            t.setValor(dto.valor());
            t.setDescricao(dto.descricao());
            t.setObservacoes(dto.observacoes());
            t.setCategoria(cat);
            return TransacaoRespostaDTO.de(t);
        }

        // caminho normal (sem cartao)
        Refs refs = validarConta(user, dto.tipo(), dto.contaId(), dto.contaDestinoId(), dto.categoriaId());

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
        Transacao t = achar(user, id);

        // pagamento de fatura: desfaz o efeito no valorPago da fatura
        if (t.getTipo() == TipoTransacao.PAGAMENTO_FATURA && t.getFatura() != null) {
            Fatura f = t.getFatura();
            f.setValorPago(f.getValorPago().subtract(t.getValor()));
            if (f.getStatus() == StatusFatura.PAGA) {
                f.setStatus(StatusFatura.FECHADA);
            }
            t.setDeletado(true);
            return;
        }

        // compra parcelada: soft-delete cascade em todas as parcelas
        if (t.getCompraParceladaId() != null) {
            repository.softDeletePorCompraParcelada(t.getCompraParceladaId(), user.getId());
            return;
        }

        t.setDeletado(true);
    }

    private Transacao achar(Usuario user, Long id) {
        return repository.findByIdAndUsuarioIdAndDeletadoFalse(id, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("transação nao encontrada"));
    }

    // ===== validacao =====

    private Refs validar(Usuario user, CriarTransacaoDTO dto) {
        TipoTransacao tipo = dto.tipo();
        Long contaId = dto.contaId();
        Long contaDestinoId = dto.contaDestinoId();
        Long categoriaId = dto.categoriaId();
        Long cartaoId = dto.cartaoId();
        Long faturaId = dto.faturaId();

        switch (tipo) {
            case RECEITA, DESPESA -> {
                if (contaDestinoId != null) {
                    throw new IllegalArgumentException("contaDestino só é permitida em transferencia");
                }
                if (faturaId != null) {
                    throw new IllegalArgumentException("faturaId só é permitido em pagamento de fatura");
                }
                boolean temConta = contaId != null;
                boolean temCartao = cartaoId != null;
                if (temConta == temCartao) {
                    throw new IllegalArgumentException("informe contaId ou cartaoId (XOR)");
                }
                if (categoriaId == null) {
                    throw new IllegalArgumentException("categoria é obrigatória pra receita/despesa");
                }
                Categoria categoria = categoriaRepository.findByIdAndUsuarioId(categoriaId, user.getId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("categoria nao encontrada"));
                TipoCategoria esperado = (tipo == TipoTransacao.RECEITA)
                        ? TipoCategoria.RECEITA : TipoCategoria.DESPESA;
                if (categoria.getTipo() != esperado) {
                    throw new IllegalArgumentException("categoria nao bate com o tipo da transação");
                }
                Conta conta = temConta ? acharConta(user, contaId) : null;
                Cartao cartao = temCartao ? acharCartao(user, cartaoId) : null;
                return new Refs(conta, null, categoria, cartao, null);
            }
            case TRANSFERENCIA -> {
                if (cartaoId != null || faturaId != null) {
                    throw new IllegalArgumentException("transferencia nao usa cartao nem fatura");
                }
                if (contaId == null) {
                    throw new IllegalArgumentException("contaId é obrigatório");
                }
                if (contaDestinoId == null) {
                    throw new IllegalArgumentException("contaDestino é obrigatória pra transferencia");
                }
                if (contaDestinoId.equals(contaId)) {
                    throw new IllegalArgumentException("conta origem e destino nao podem ser iguais");
                }
                if (categoriaId != null) {
                    throw new IllegalArgumentException("transferencia nao tem categoria");
                }
                Conta conta = acharConta(user, contaId);
                Conta contaDestino = acharConta(user, contaDestinoId);
                return new Refs(conta, contaDestino, null, null, null);
            }
            case PAGAMENTO_FATURA -> {
                if (contaId == null || faturaId == null) {
                    throw new IllegalArgumentException("contaId e faturaId sao obrigatorios pra pagamento de fatura");
                }
                if (cartaoId != null) {
                    throw new IllegalArgumentException("nao informe cartaoId em pagamento de fatura");
                }
                if (categoriaId != null) {
                    throw new IllegalArgumentException("pagamento de fatura nao tem categoria");
                }
                if (contaDestinoId != null) {
                    throw new IllegalArgumentException("pagamento de fatura nao tem contaDestino");
                }
                Conta conta = acharConta(user, contaId);
                Fatura fatura = faturaRepository.findByIdEUsuarioId(faturaId, user.getId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("fatura nao encontrada"));
                if (fatura.getStatus() == StatusFatura.PAGA) {
                    throw new IllegalArgumentException("fatura ja esta paga");
                }
                return new Refs(conta, null, null, null, fatura);
            }
        }
        throw new IllegalStateException("tipo de transacao nao tratado: " + tipo);
    }

    // versao simples pra atualizar (sem cartao/fatura)
    private Refs validarConta(Usuario user, TipoTransacao tipo, Long contaId, Long contaDestinoId, Long categoriaId) {
        if (contaId == null) {
            throw new IllegalArgumentException("contaId é obrigatório");
        }
        Conta conta = acharConta(user, contaId);
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
                        ? TipoCategoria.RECEITA : TipoCategoria.DESPESA;
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
                contaDestino = acharConta(user, contaDestinoId);
            }
            default -> throw new IllegalArgumentException("tipo invalido na edicao: " + tipo);
        }

        return new Refs(conta, contaDestino, categoria, null, null);
    }

    private Conta acharConta(Usuario user, Long id) {
        return contaRepository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("conta nao encontrada"));
    }

    private Cartao acharCartao(Usuario user, Long id) {
        return cartaoRepository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("cartao nao encontrado"));
    }

    private void aplicarPagamentoNaFatura(Fatura f, BigDecimal valor) {
        f.setValorPago(f.getValorPago().add(valor));
        // se ja cobriu o total, marca como paga.
        // o valorTotal e dinamico (vem das transacoes), entao recalculo aqui
        BigDecimal despesas = faturaRepository.somarDespesasDaFatura(f.getId());
        BigDecimal estornos = faturaRepository.somarEstornosDaFatura(f.getId());
        BigDecimal total = despesas.subtract(estornos);
        if (f.getValorPago().compareTo(total) >= 0 && total.signum() > 0) {
            f.setStatus(StatusFatura.PAGA);
        } else if (f.getStatus() == StatusFatura.ABERTA) {
            // pagamento antecipado mantem aberta. so transiciona quando fechar de fato
        }
    }

    private record Refs(Conta conta, Conta contaDestino, Categoria categoria, Cartao cartao, Fatura fatura) {
    }
}
