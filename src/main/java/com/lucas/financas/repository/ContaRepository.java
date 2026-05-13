package com.lucas.financas.repository;

import com.lucas.financas.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContaRepository extends JpaRepository<Conta, Long> {

    List<Conta> findByUsuarioIdAndAtivaTrueOrderByNome(Long usuarioId);

    // sempre filtra pelo usuario pra ninguem ver conta de outro
    Optional<Conta> findByIdAndUsuarioId(Long id, Long usuarioId);
}
