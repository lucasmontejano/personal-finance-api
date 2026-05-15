package com.lucas.financas.repository;

import com.lucas.financas.model.Cartao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartaoRepository extends JpaRepository<Cartao, Long> {

    List<Cartao> findByUsuarioIdAndAtivoTrueOrderByNome(Long usuarioId);

    Optional<Cartao> findByIdAndUsuarioId(Long id, Long usuarioId);
}
