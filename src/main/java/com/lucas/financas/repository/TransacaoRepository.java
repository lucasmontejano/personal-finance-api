package com.lucas.financas.repository;

import com.lucas.financas.model.Transacao;
import com.lucas.financas.model.TipoTransacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    Optional<Transacao> findByIdAndUsuarioIdAndDeletadoFalse(Long id, Long usuarioId);

    Optional<Transacao> findByUsuarioIdAndIdempotencyKey(Long usuarioId, String idempotencyKey);

    boolean existsByCategoriaIdAndDeletadoFalse(Long categoriaId);

    @Query("""
            SELECT t FROM Transacao t
            WHERE t.usuario.id = :usuarioId
              AND t.deletado = false
              AND (:contaId IS NULL OR t.conta.id = :contaId OR t.contaDestino.id = :contaId)
              AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId)
              AND (:tipo IS NULL OR t.tipo = :tipo)
              AND (:inicio IS NULL OR t.data >= :inicio)
              AND (:fim IS NULL OR t.data <= :fim)
            ORDER BY t.data DESC, t.id DESC
            """)
    Page<Transacao> buscar(
            @Param("usuarioId") Long usuarioId,
            @Param("contaId") Long contaId,
            @Param("categoriaId") Long categoriaId,
            @Param("tipo") TipoTransacao tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            Pageable pageable
    );

    // soma valores onde a conta é origem (RECEITA / DESPESA / saidas de TRANSFERENCIA)
    @Query("""
            SELECT COALESCE(SUM(t.valor), 0)
            FROM Transacao t
            WHERE t.conta.id = :contaId
              AND t.tipo = :tipo
              AND t.deletado = false
            """)
    BigDecimal somarPorContaETipo(@Param("contaId") Long contaId, @Param("tipo") TipoTransacao tipo);

    // soma valores onde a conta é o destino da transferencia
    @Query("""
            SELECT COALESCE(SUM(t.valor), 0)
            FROM Transacao t
            WHERE t.contaDestino.id = :contaId
              AND t.tipo = :tipo
              AND t.deletado = false
            """)
    BigDecimal somarPorContaDestinoETipo(@Param("contaId") Long contaId, @Param("tipo") TipoTransacao tipo);

    // agrupa por categoria - serve pra orçamento (DESPESA) e pra relatorio (RECEITA ou DESPESA)
    @Query("""
            SELECT t.categoria.id AS categoriaId, SUM(t.valor) AS total
            FROM Transacao t
            WHERE t.usuario.id = :usuarioId
              AND t.tipo = :tipo
              AND t.data BETWEEN :inicio AND :fim
              AND t.deletado = false
            GROUP BY t.categoria.id
            """)
    List<GastoPorCategoria> agregaPorCategoriaETipo(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TipoTransacao tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim
    );

    // soma total do tipo num periodo - pro resumo mensal
    @Query("""
            SELECT COALESCE(SUM(t.valor), 0)
            FROM Transacao t
            WHERE t.usuario.id = :usuarioId
              AND t.tipo = :tipo
              AND t.data BETWEEN :inicio AND :fim
              AND t.deletado = false
            """)
    BigDecimal somarPorUsuarioTipoEPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("tipo") TipoTransacao tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim
    );

    long countByUsuarioIdAndTipoAndDataBetweenAndDeletadoFalse(
            Long usuarioId, TipoTransacao tipo, LocalDate inicio, LocalDate fim
    );

    // pega as transacoes do periodo (receita + despesa) pra montar fluxo de caixa em Java
    @Query("""
            SELECT t FROM Transacao t
            WHERE t.usuario.id = :usuarioId
              AND t.data BETWEEN :inicio AND :fim
              AND t.tipo IN :tipos
              AND t.deletado = false
            ORDER BY t.data ASC
            """)
    List<Transacao> buscarPorPeriodoETipos(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("tipos") List<TipoTransacao> tipos
    );
}
