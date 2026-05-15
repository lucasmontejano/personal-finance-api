package com.lucas.financas.repository;

import com.lucas.financas.model.Fatura;
import com.lucas.financas.model.StatusFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface FaturaRepository extends JpaRepository<Fatura, Long> {

    List<Fatura> findByCartaoIdOrderByMesReferenciaDesc(Long cartaoId);

    Optional<Fatura> findByCartaoIdAndMesReferencia(Long cartaoId, LocalDate mesReferencia);

    // multi-tenant: pega a fatura garantindo que o cartao eh do usuario
    @Query("""
        SELECT f FROM Fatura f
        WHERE f.id = :id AND f.cartao.usuario.id = :usuarioId
    """)
    Optional<Fatura> findByIdEUsuarioId(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    // soma dos valores das transacoes da fatura (calculado, nao persistido)
    @Query("""
        SELECT COALESCE(SUM(t.valor), 0)
        FROM Transacao t
        WHERE t.fatura.id = :faturaId AND t.deletado = false AND t.tipo = com.lucas.financas.model.TipoTransacao.DESPESA
    """)
    BigDecimal somarDespesasDaFatura(@Param("faturaId") Long faturaId);

    // estorno (RECEITA com cartao) reduz o total
    @Query("""
        SELECT COALESCE(SUM(t.valor), 0)
        FROM Transacao t
        WHERE t.fatura.id = :faturaId AND t.deletado = false AND t.tipo = com.lucas.financas.model.TipoTransacao.RECEITA
    """)
    BigDecimal somarEstornosDaFatura(@Param("faturaId") Long faturaId);

    // soma do limite usado de um cartao = todas as faturas nao pagas
    @Query("""
        SELECT COALESCE(SUM(
            (SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t
              WHERE t.fatura.id = f.id AND t.deletado = false AND t.tipo = com.lucas.financas.model.TipoTransacao.DESPESA)
            - (SELECT COALESCE(SUM(t.valor), 0) FROM Transacao t
              WHERE t.fatura.id = f.id AND t.deletado = false AND t.tipo = com.lucas.financas.model.TipoTransacao.RECEITA)
            - f.valorPago
        ), 0)
        FROM Fatura f
        WHERE f.cartao.id = :cartaoId AND f.status <> com.lucas.financas.model.StatusFatura.PAGA
    """)
    BigDecimal calcularLimiteUsado(@Param("cartaoId") Long cartaoId);

    // pra bloquear soft-delete de cartao com fatura pendente
    boolean existsByCartaoIdAndStatusNot(Long cartaoId, StatusFatura status);

    // faturas ABERTAS cuja data de fechamento ja chegou — o scheduler fecha
    List<Fatura> findByStatusAndDataFechamentoLessThanEqual(StatusFatura status, LocalDate data);
}
