package com.lucas.financas.controller;

import com.lucas.financas.dto.AtualizarTransacaoRecorrenteDTO;
import com.lucas.financas.dto.CriarTransacaoRecorrenteDTO;
import com.lucas.financas.dto.TransacaoRecorrenteRespostaDTO;
import com.lucas.financas.dto.TransacaoRespostaDTO;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.TransacaoRecorrenteService;
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
@RequestMapping("/transacoes-recorrentes")
public class TransacaoRecorrenteController {

    private final TransacaoRecorrenteService service;

    public TransacaoRecorrenteController(TransacaoRecorrenteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TransacaoRecorrenteRespostaDTO>> listar(@AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(service.listar(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoRecorrenteRespostaDTO> buscar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscar(user, id));
    }

    @PostMapping
    public ResponseEntity<TransacaoRecorrenteRespostaDTO> criar(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid CriarTransacaoRecorrenteDTO dto,
            UriComponentsBuilder uri
    ) {
        TransacaoRecorrenteRespostaDTO criada = service.criar(user, dto);
        URI location = uri.path("/transacoes-recorrentes/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransacaoRecorrenteRespostaDTO> atualizar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id,
            @RequestBody @Valid AtualizarTransacaoRecorrenteDTO dto
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

    // dispara a execução manual pro mes atual. idempotente
    @PostMapping("/{id}/executar-agora")
    public ResponseEntity<TransacaoRespostaDTO> executarAgora(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.executarAgora(user, id));
    }
}
