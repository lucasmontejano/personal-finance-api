package com.lucas.financas.controller;

import com.lucas.financas.dto.AtualizarCartaoDTO;
import com.lucas.financas.dto.CartaoRespostaDTO;
import com.lucas.financas.dto.CriarCartaoDTO;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.CartaoService;
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
@RequestMapping("/cartoes")
public class CartaoController {

    private final CartaoService service;

    public CartaoController(CartaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CartaoRespostaDTO>> listar(@AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(service.listar(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartaoRespostaDTO> buscar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscar(user, id));
    }

    @PostMapping
    public ResponseEntity<CartaoRespostaDTO> criar(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid CriarCartaoDTO dto,
            UriComponentsBuilder uri
    ) {
        CartaoRespostaDTO criado = service.criar(user, dto);
        URI location = uri.path("/cartoes/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartaoRespostaDTO> atualizar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id,
            @RequestBody @Valid AtualizarCartaoDTO dto
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
