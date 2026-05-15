package com.lucas.financas.service;

import com.lucas.financas.dto.AtualizarCartaoDTO;
import com.lucas.financas.dto.CartaoRespostaDTO;
import com.lucas.financas.dto.CriarCartaoDTO;
import com.lucas.financas.exception.RecursoNaoEncontradoException;
import com.lucas.financas.model.Cartao;
import com.lucas.financas.model.Conta;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.CartaoRepository;
import com.lucas.financas.repository.ContaRepository;
import com.lucas.financas.repository.FaturaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartaoService {

    private final CartaoRepository repository;
    private final ContaRepository contaRepository;
    private final FaturaRepository faturaRepository;

    public CartaoService(CartaoRepository repository, ContaRepository contaRepository, FaturaRepository faturaRepository) {
        this.repository = repository;
        this.contaRepository = contaRepository;
        this.faturaRepository = faturaRepository;
    }

    @Transactional
    public CartaoRespostaDTO criar(Usuario usuario, CriarCartaoDTO dto) {
        Conta contaPadrao = resolverContaPadrao(usuario, dto.contaPadraoPagamentoId());
        Cartao c = new Cartao(
                usuario,
                dto.nome(),
                dto.bandeira(),
                dto.limite(),
                dto.diaFechamento(),
                dto.diaVencimento(),
                contaPadrao,
                dto.cor()
        );
        Cartao salvo = repository.save(c);
        // cartao novo nao tem fatura ainda, limite usado = 0
        return CartaoRespostaDTO.de(salvo, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<CartaoRespostaDTO> listar(Usuario usuario) {
        return repository.findByUsuarioIdAndAtivoTrueOrderByNome(usuario.getId())
                .stream()
                .map(c -> CartaoRespostaDTO.de(c, calcularLimiteUsado(c)))
                .toList();
    }

    @Transactional(readOnly = true)
    public CartaoRespostaDTO buscar(Usuario usuario, Long id) {
        Cartao c = achar(usuario, id);
        return CartaoRespostaDTO.de(c, calcularLimiteUsado(c));
    }

    @Transactional
    public CartaoRespostaDTO atualizar(Usuario usuario, Long id, AtualizarCartaoDTO dto) {
        Cartao c = achar(usuario, id);
        c.setNome(dto.nome());
        c.setBandeira(dto.bandeira());
        c.setLimite(dto.limite());
        c.setDiaFechamento(dto.diaFechamento());
        c.setDiaVencimento(dto.diaVencimento());
        c.setContaPadraoPagamento(resolverContaPadrao(usuario, dto.contaPadraoPagamentoId()));
        c.setCor(dto.cor());
        return CartaoRespostaDTO.de(c, calcularLimiteUsado(c));
    }

    @Transactional
    public void deletar(Usuario usuario, Long id) {
        Cartao c = achar(usuario, id);
        // se tem fatura nao paga (ABERTA ou FECHADA), nao pode deletar
        if (faturaRepository.existsByCartaoIdAndStatusNot(c.getId(), com.lucas.financas.model.StatusFatura.PAGA)) {
            throw new IllegalArgumentException("nao pode deletar cartao com fatura em aberto ou nao paga");
        }
        c.setAtivo(false);
    }

    private Cartao achar(Usuario usuario, Long id) {
        return repository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("cartao nao encontrado"));
    }

    private Conta resolverContaPadrao(Usuario usuario, Long contaId) {
        if (contaId == null) return null;
        return contaRepository.findByIdAndUsuarioId(contaId, usuario.getId())
                .orElseThrow(() -> new IllegalArgumentException("conta padrao de pagamento nao encontrada"));
    }

    // soma do valor pendente de todas as faturas nao pagas
    private BigDecimal calcularLimiteUsado(Cartao c) {
        BigDecimal usado = faturaRepository.calcularLimiteUsado(c.getId());
        // se nao tem fatura ainda, vem 0; tambem nao deixo negativo (estorno isolado)
        return usado.signum() < 0 ? BigDecimal.ZERO : usado;
    }
}
