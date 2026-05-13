package com.lucas.financas.controller;

import com.lucas.financas.dto.AtualizarTransacaoDTO;
import com.lucas.financas.dto.CriarTransacaoDTO;
import com.lucas.financas.dto.PaginaDTO;
import com.lucas.financas.dto.TransacaoRespostaDTO;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    private final TransacaoService service;

    public TransacaoController(TransacaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PaginaDTO<TransacaoRespostaDTO>> listar(
            @AuthenticationPrincipal Usuario user,
            @RequestParam(required = false) Long contaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) TipoTransacao tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(service.listar(user, contaId, categoriaId, tipo, inicio, fim, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransacaoRespostaDTO> buscar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscar(user, id));
    }

    @PostMapping
    public ResponseEntity<TransacaoRespostaDTO> criar(
            @AuthenticationPrincipal Usuario user,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody @Valid CriarTransacaoDTO dto,
            UriComponentsBuilder uri
    ) {
        TransacaoRespostaDTO criada = service.criar(user, idempotencyKey, dto);
        URI location = uri.path("/transacoes/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransacaoRespostaDTO> atualizar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id,
            @RequestBody @Valid AtualizarTransacaoDTO dto
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
