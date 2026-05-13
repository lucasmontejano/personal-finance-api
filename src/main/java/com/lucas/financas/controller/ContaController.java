package com.lucas.financas.controller;

import com.lucas.financas.dto.AtualizarContaDTO;
import com.lucas.financas.dto.ContaRespostaDTO;
import com.lucas.financas.dto.CriarContaDTO;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.ContaService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ContaRespostaDTO>> listar(@AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(service.listar(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContaRespostaDTO> buscar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscar(user, id));
    }

    @PostMapping
    public ResponseEntity<ContaRespostaDTO> criar(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid CriarContaDTO dto,
            UriComponentsBuilder uri
    ) {
        ContaRespostaDTO criada = service.criar(user, dto);
        URI location = uri.path("/contas/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContaRespostaDTO> atualizar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id,
            @RequestBody @Valid AtualizarContaDTO dto
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
