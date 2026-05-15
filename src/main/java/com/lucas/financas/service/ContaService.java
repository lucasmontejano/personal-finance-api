package com.lucas.financas.service;

import com.lucas.financas.dto.AtualizarContaDTO;
import com.lucas.financas.dto.ContaRespostaDTO;
import com.lucas.financas.dto.CriarContaDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Conta;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.ContaRepository;
import com.lucas.financas.repository.TransacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ContaService {

    private final ContaRepository repository;
    private final TransacaoRepository transacaoRepository;

    public ContaService(ContaRepository repository, TransacaoRepository transacaoRepository) {
        this.repository = repository;
        this.transacaoRepository = transacaoRepository;
    }

    @Transactional
    public ContaRespostaDTO criar(Usuario usuario, CriarContaDTO dto) {
        Conta c = new Conta(usuario, dto.nome(), dto.tipo(), dto.saldoInicial(), dto.cor());
        Conta salva = repository.save(c);
        // conta nova ainda nao tem transação, saldo = inicial
        return ContaRespostaDTO.de(salva, salva.getSaldoInicial());
    }

    @Transactional(readOnly = true)
    public List<ContaRespostaDTO> listar(Usuario usuario) {
        return repository.findByUsuarioIdAndAtivaTrueOrderByNome(usuario.getId())
                .stream()
                .map(c -> ContaRespostaDTO.de(c, calcularSaldo(c)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContaRespostaDTO buscar(Usuario usuario, Long id) {
        Conta c = achar(usuario, id);
        return ContaRespostaDTO.de(c, calcularSaldo(c));
    }

    @Transactional
    public ContaRespostaDTO atualizar(Usuario usuario, Long id, AtualizarContaDTO dto) {
        Conta c = achar(usuario, id);
        c.setNome(dto.nome());
        c.setCor(dto.cor());
        return ContaRespostaDTO.de(c, calcularSaldo(c));
    }

    @Transactional
    public void deletar(Usuario usuario, Long id) {
        // soft delete - mantem o historico de transacoes
        Conta c = achar(usuario, id);
        c.setAtiva(false);
    }

    private Conta achar(Usuario usuario, Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("conta nao encontrada"));
    }

    // saldo = inicial + receitas - despesas + transferencias recebidas - transferencias enviadas - pagamentos de fatura
    // (transacoes de cartao tem conta_id null entao nao caem em nenhuma das somas — sai certo de graça)
    private BigDecimal calcularSaldo(Conta c) {
        Long id = c.getId();
        BigDecimal receitas = transacaoRepository.somarPorContaETipo(id, TipoTransacao.RECEITA);
        BigDecimal despesas = transacaoRepository.somarPorContaETipo(id, TipoTransacao.DESPESA);
        BigDecimal saidasTransfer = transacaoRepository.somarPorContaETipo(id, TipoTransacao.TRANSFERENCIA);
        BigDecimal entradasTransfer = transacaoRepository.somarPorContaDestinoETipo(id, TipoTransacao.TRANSFERENCIA);
        BigDecimal pagFatura = transacaoRepository.somarPorContaETipo(id, TipoTransacao.PAGAMENTO_FATURA);

        return c.getSaldoInicial()
                .add(receitas)
                .subtract(despesas)
                .add(entradasTransfer)
                .subtract(saidasTransfer)
                .subtract(pagFatura);
    }
}
