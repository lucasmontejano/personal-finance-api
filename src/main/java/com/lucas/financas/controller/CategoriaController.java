package com.lucas.financas.controller;

import com.lucas.financas.dto.AtualizarCategoriaDTO;
import com.lucas.financas.dto.CategoriaRespostaDTO;
import com.lucas.financas.dto.CriarCategoriaDTO;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaRespostaDTO>> listar(
            @AuthenticationPrincipal Usuario user,
            @RequestParam(required = false) TipoCategoria tipo
    ) {
        return ResponseEntity.ok(service.listar(user, tipo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaRespostaDTO> buscar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscar(user, id));
    }

    @PostMapping
    public ResponseEntity<CategoriaRespostaDTO> criar(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid CriarCategoriaDTO dto,
            UriComponentsBuilder uri
    ) {
        CategoriaRespostaDTO criada = service.criar(user, dto);
        URI location = uri.path("/categorias/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaRespostaDTO> atualizar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id,
            @RequestBody @Valid AtualizarCategoriaDTO dto
    ) {
        return ResponseEntity.ok(service.atualizar(user, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        service.deletar(user, id);
        return ResponseEntity.noContent().build();
    }
}
