package com.lucas.financas.controller;

import com.lucas.financas.dto.AtualizarOrcamentoDTO;
import com.lucas.financas.dto.ComparativoMesDTO;
import com.lucas.financas.dto.CriarOrcamentoDTO;
import com.lucas.financas.dto.OrcamentoRespostaDTO;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.OrcamentoService;
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
@RequestMapping("/orcamentos")
public class OrcamentoController {

    private final OrcamentoService service;

    public OrcamentoController(OrcamentoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<OrcamentoRespostaDTO>> listar(
            @AuthenticationPrincipal Usuario user,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes
    ) {
        return ResponseEntity.ok(service.listar(user, ano, mes));
    }

    @GetMapping("/comparativo")
    public ResponseEntity<ComparativoMesDTO> comparativo(
            @AuthenticationPrincipal Usuario user,
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        return ResponseEntity.ok(service.comparativo(user, ano, mes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrcamentoRespostaDTO> buscar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscar(user, id));
    }

    @PostMapping
    public ResponseEntity<OrcamentoRespostaDTO> criar(
            @AuthenticationPrincipal Usuario user,
            @RequestBody @Valid CriarOrcamentoDTO dto,
            UriComponentsBuilder uri
    ) {
        OrcamentoRespostaDTO criado = service.criar(user, dto);
        URI location = uri.path("/orcamentos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrcamentoRespostaDTO> atualizar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id,
            @RequestBody @Valid AtualizarOrcamentoDTO dto
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
