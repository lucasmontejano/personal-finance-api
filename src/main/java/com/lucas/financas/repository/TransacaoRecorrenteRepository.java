package com.lucas.financas.repository;

import com.lucas.financas.model.TransacaoRecorrente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransacaoRecorrenteRepository extends JpaRepository<TransacaoRecorrente, Long> {

    List<TransacaoRecorrente> findByUsuarioIdOrderByDescricaoAsc(Long usuarioId);

    Optional<TransacaoRecorrente> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<TransacaoRecorrente> findByAtivaTrue();
}
