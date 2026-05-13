package com.lucas.financas.repository;

import com.lucas.financas.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    Optional<Orcamento> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<Orcamento> findByUsuarioIdOrderByMesReferenciaDesc(Long usuarioId);

    List<Orcamento> findByUsuarioIdAndMesReferenciaOrderByCategoriaNomeAsc(Long usuarioId, LocalDate mes);

    boolean existsByUsuarioIdAndCategoriaIdAndMesReferencia(Long usuarioId, Long categoriaId, LocalDate mes);
}
