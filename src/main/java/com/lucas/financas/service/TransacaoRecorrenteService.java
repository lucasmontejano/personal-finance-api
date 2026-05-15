package com.lucas.financas.service;

import com.lucas.financas.dto.AtualizarTransacaoRecorrenteDTO;
import com.lucas.financas.dto.CriarTransacaoDTO;
import com.lucas.financas.dto.CriarTransacaoRecorrenteDTO;
import com.lucas.financas.dto.TransacaoRecorrenteRespostaDTO;
import com.lucas.financas.dto.TransacaoRespostaDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.Conta;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.TransacaoRecorrente;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CategoriaRepository;
import com.lucas.financas.repository.ContaRepository;
import com.lucas.financas.repository.TransacaoRecorrenteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransacaoRecorrenteService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoRecorrenteService.class);

    private final TransacaoRecorrenteRepository repository;
    private final ContaRepository contaRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransacaoService transacaoService;

    public TransacaoRecorrenteService(
            TransacaoRecorrenteRepository repository,
            ContaRepository contaRepository,
            CategoriaRepository categoriaRepository,
            TransacaoService transacaoService
    ) {
        this.repository = repository;
        this.contaRepository = contaRepository;
        this.categoriaRepository = categoriaRepository;
        this.transacaoService = transacaoService;
    }

    @Transactional
    public TransacaoRecorrenteRespostaDTO criar(Usuario user, CriarTransacaoRecorrenteDTO dto) {
        validarPeriodo(dto.dataInicio(), dto.dataFim());
        Refs refs = validar(user, dto.tipo(), dto.contaId(), dto.contaDestinoId(), dto.categoriaId());

        TransacaoRecorrente r = new TransacaoRecorrente(
                user,
                refs.conta(),
                refs.contaDestino(),
                refs.categoria(),
                dto.tipo(),
                dto.valor(),
                dto.descricao(),
                dto.diaDoMes(),
                dto.dataInicio(),
                dto.dataFim()
        );
        return TransacaoRecorrenteRespostaDTO.de(repository.save(r));
    }

    @Transactional(readOnly = true)
    public List<TransacaoRecorrenteRespostaDTO> listar(Usuario user) {
        return repository.findByUsuarioIdOrderByDescricaoAsc(user.getId())
                .stream()
                .map(TransacaoRecorrenteRespostaDTO::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransacaoRecorrenteRespostaDTO buscar(Usuario user, Long id) {
        return TransacaoRecorrenteRespostaDTO.de(achar(user, id));
    }

    @Transactional
    public TransacaoRecorrenteRespostaDTO atualizar(Usuario user, Long id, AtualizarTransacaoRecorrenteDTO dto) {
        TransacaoRecorrente r = achar(user, id);
        validarPeriodo(dto.dataInicio(), dto.dataFim());
        Refs refs = validar(user, dto.tipo(), dto.contaId(), dto.contaDestinoId(), dto.categoriaId());

        r.setTipo(dto.tipo());
        r.setValor(dto.valor());
        r.setDescricao(dto.descricao());
        r.setConta(refs.conta());
        r.setContaDestino(refs.contaDestino());
        r.setCategoria(refs.categoria());
        r.setDiaDoMes(dto.diaDoMes());
        r.setDataInicio(dto.dataInicio());
        r.setDataFim(dto.dataFim());
        r.setAtiva(dto.ativa());

        return TransacaoRecorrenteRespostaDTO.de(r);
    }

    @Transactional
    public void deletar(Usuario user, Long id) {
        TransacaoRecorrente r = achar(user, id);
        repository.delete(r);
    }

    // dispara execução manual pra hoje. idempotente: se ja rodou esse mes, devolve a mesma transacao
    @Transactional
    public TransacaoRespostaDTO executarAgora(Usuario user, Long id) {
        TransacaoRecorrente r = achar(user, id);
        if (!r.isAtiva()) {
            throw new IllegalArgumentException("recorrencia esta pausada");
        }
        return executar(r, LocalDate.now());
    }

    // chamado pelo scheduler. processa todas as recorrencias ativas que devem rodar hoje
    @Transactional
    public int processarPendentes(LocalDate hoje) {
        List<TransacaoRecorrente> ativas = repository.findByAtivaTrue();
        int executadas = 0;
        for (TransacaoRecorrente r : ativas) {
            try {
                if (deveExecutar(r, hoje)) {
                    executar(r, hoje);
                    executadas++;
                }
            } catch (Exception e) {
                // se uma falhar, segue pras outras
                log.warn("falhou ao executar recorrencia {}: {}", r.getId(), e.getMessage());
            }
        }
        return executadas;
    }

    // checa se a recorrencia deve rodar hoje
    private boolean deveExecutar(TransacaoRecorrente r, LocalDate hoje) {
        if (hoje.isBefore(r.getDataInicio())) return false;
        if (r.getDataFim() != null && hoje.isAfter(r.getDataFim())) return false;

        int diaEfetivo = Math.min(r.getDiaDoMes(), hoje.lengthOfMonth());
        if (hoje.getDayOfMonth() != diaEfetivo) return false;

        // se ja rodou nesse mes, pula
        LocalDate ultima = r.getUltimaExecucao();
        if (ultima != null && ultima.getYear() == hoje.getYear() && ultima.getMonthValue() == hoje.getMonthValue()) {
            return false;
        }
        return true;
    }

    // gera a transacao pro mes da data referencia. idempotency garante que nao duplica
    private TransacaoRespostaDTO executar(TransacaoRecorrente r, LocalDate referencia) {
        int diaEfetivo = Math.min(r.getDiaDoMes(), referencia.lengthOfMonth());
        LocalDate dataTx = LocalDate.of(referencia.getYear(), referencia.getMonth(), diaEfetivo);

        String idemKey = "rec_" + r.getId() + "_" + referencia.getYear() + "-" + referencia.getMonthValue();

        CriarTransacaoDTO novaTx = new CriarTransacaoDTO(
                r.getTipo(),
                r.getValor(),
                r.getDescricao(),
                dataTx,
                r.getConta().getId(),
                r.getContaDestino() != null ? r.getContaDestino().getId() : null,
                r.getCategoria() != null ? r.getCategoria().getId() : null,
                null,
                null,
                1,
                "gerada pela recorrencia #" + r.getId()
        );

        TransacaoRespostaDTO resp = transacaoService.criar(r.getUsuario(), idemKey, novaTx);
        r.setUltimaExecucao(referencia);
        return resp;
    }

    private TransacaoRecorrente achar(Usuario user, Long id) {
        return repository.findByIdAndUsuarioId(id, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("recorrencia nao encontrada"));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("dataFim nao pode ser antes de dataInicio");
        }
    }

    // mesma logica do TransacaoService — duplicada de propósito pra nao acoplar os services
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
            // PAGAMENTO_FATURA nao faz sentido em recorrencia (faturaId muda todo mes)
            default -> throw new IllegalArgumentException("tipo " + tipo + " nao suportado em recorrencia");
        }

        return new Refs(conta, contaDestino, categoria);
    }

    private record Refs(Conta conta, Conta contaDestino, Categoria categoria) {
    }
}
