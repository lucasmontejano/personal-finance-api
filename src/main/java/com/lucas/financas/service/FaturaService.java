package com.lucas.financas.service;

import com.lucas.financas.dto.FaturaRespostaDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Cartao;
import com.lucas.financas.model.Fatura;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CartaoRepository;
import com.lucas.financas.repository.FaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class FaturaService {

    private final FaturaRepository repository;
    private final CartaoRepository cartaoRepository;

    public FaturaService(FaturaRepository repository, CartaoRepository cartaoRepository) {
        this.repository = repository;
        this.cartaoRepository = cartaoRepository;
    }

    // descobre (ou cria lazy) a fatura que uma compra deve entrar baseado na data
    @Transactional
    public Fatura findOuCriaPorData(Cartao cartao, LocalDate dataCompra) {
        LocalDate mesRef = calcularMesReferencia(dataCompra, cartao);
        return repository.findByCartaoIdAndMesReferencia(cartao.getId(), mesRef)
                .orElseGet(() -> {
                    LocalDate fech = calcularDataFechamento(mesRef, cartao);
                    LocalDate venc = calcularDataVencimento(mesRef, cartao);
                    return repository.save(new Fatura(cartao, mesRef, fech, venc));
                });
    }

    @Transactional(readOnly = true)
    public List<FaturaRespostaDTO> listarPorCartao(Usuario user, Long cartaoId) {
        Cartao cartao = acharCartao(user, cartaoId);
        return repository.findByCartaoIdOrderByMesReferenciaDesc(cartao.getId())
                .stream()
                .map(f -> FaturaRespostaDTO.de(f, valorTotalDaFatura(f.getId())))
                .toList();
    }

    // "atual" = a fatura onde uma compra de hoje cairia. cria lazy se ainda nao existe.
    // (varias faturas podem estar ABERTAS ao mesmo tempo por causa de parcelamento futuro,
    //  entao "qualquer ABERTA" nao serve — preciso da que corresponde a hoje)
    @Transactional
    public FaturaRespostaDTO buscarAtual(Usuario user, Long cartaoId) {
        Cartao cartao = acharCartao(user, cartaoId);
        Fatura f = findOuCriaPorData(cartao, LocalDate.now());
        return FaturaRespostaDTO.de(f, valorTotalDaFatura(f.getId()));
    }

    @Transactional(readOnly = true)
    public FaturaRespostaDTO buscar(Usuario user, Long faturaId) {
        Fatura f = repository.findByIdEUsuarioId(faturaId, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("fatura nao encontrada"));
        return FaturaRespostaDTO.de(f, valorTotalDaFatura(f.getId()));
    }

    // forca fechamento manual de uma fatura — usado pra testar / debugar (smoke chama isso)
    @Transactional
    public FaturaRespostaDTO forcarFechamento(Usuario user, Long faturaId) {
        Fatura f = repository.findByIdEUsuarioId(faturaId, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("fatura nao encontrada"));
        if (f.getStatus() == com.lucas.financas.model.StatusFatura.ABERTA) {
            f.setStatus(com.lucas.financas.model.StatusFatura.FECHADA);
        }
        return FaturaRespostaDTO.de(f, valorTotalDaFatura(f.getId()));
    }

    // total = soma das despesas - soma dos estornos
    @Transactional(readOnly = true)
    public BigDecimal valorTotalDaFatura(Long faturaId) {
        BigDecimal despesas = repository.somarDespesasDaFatura(faturaId);
        BigDecimal estornos = repository.somarEstornosDaFatura(faturaId);
        return despesas.subtract(estornos);
    }

    // ===== regras de calculo de fatura =====

    // dado uma data de compra, descobre o mes_referencia da fatura
    LocalDate calcularMesReferencia(LocalDate dataCompra, Cartao c) {
        LocalDate candidato = dataCompra.withDayOfMonth(1);
        // procura ate 4 meses a frente — mais que isso eh bug
        for (int i = 0; i < 4; i++) {
            LocalDate dataFech = calcularDataFechamento(candidato, c);
            if (!dataCompra.isAfter(dataFech)) {
                return candidato;
            }
            candidato = candidato.plusMonths(1);
        }
        throw new IllegalStateException("nao consegui calcular mes da fatura pra " + dataCompra);
    }

    LocalDate calcularDataFechamento(LocalDate mesReferencia, Cartao c) {
        // se vencimento > fechamento, fechamento no mesmo mes da venc (case A)
        // se vencimento <= fechamento, fechamento no mes anterior (case B)
        LocalDate mesFechamento = (c.getDiaVencimento() > c.getDiaFechamento())
                ? mesReferencia
                : mesReferencia.minusMonths(1);
        int dia = Math.min(c.getDiaFechamento(), mesFechamento.lengthOfMonth());
        return mesFechamento.withDayOfMonth(dia);
    }

    LocalDate calcularDataVencimento(LocalDate mesReferencia, Cartao c) {
        int dia = Math.min(c.getDiaVencimento(), mesReferencia.lengthOfMonth());
        return mesReferencia.withDayOfMonth(dia);
    }

    private Cartao acharCartao(Usuario user, Long cartaoId) {
        return cartaoRepository.findByIdAndUsuarioId(cartaoId, user.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("cartao nao encontrado"));
    }
}
