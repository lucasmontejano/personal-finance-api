package com.lucas.financas.repository;

import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.TipoCategoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByUsuarioIdOrderByTipoAscNomeAsc(Long usuarioId);

    List<Categoria> findByUsuarioIdAndTipoOrderByNome(Long usuarioId, TipoCategoria tipo);

    Optional<Categoria> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndTipo(Long usuarioId, String nome, TipoCategoria tipo);
}
