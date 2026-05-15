package com.lucas.financas.controller;

import com.lucas.financas.dto.CriarTransacaoDTO;
import com.lucas.financas.dto.FaturaRespostaDTO;
import com.lucas.financas.dto.PagarFaturaDTO;
import com.lucas.financas.dto.TransacaoRespostaDTO;
import com.lucas.financas.model.TipoTransacao;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.FaturaService;
import com.lucas.financas.service.TransacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class FaturaController {

    private final FaturaService service;
    private final TransacaoService transacaoService;

    public FaturaController(FaturaService service, TransacaoService transacaoService) {
        this.service = service;
        this.transacaoService = transacaoService;
    }

    @GetMapping("/cartoes/{cartaoId}/faturas")
    public ResponseEntity<List<FaturaRespostaDTO>> listar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long cartaoId
    ) {
        return ResponseEntity.ok(service.listarPorCartao(user, cartaoId));
    }

    @GetMapping("/cartoes/{cartaoId}/faturas/atual")
    public ResponseEntity<FaturaRespostaDTO> atual(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long cartaoId
    ) {
        return ResponseEntity.ok(service.buscarAtual(user, cartaoId));
    }

    @GetMapping("/faturas/{id}")
    public ResponseEntity<FaturaRespostaDTO> buscar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.buscar(user, id));
    }

    @PostMapping("/faturas/{id}/forcar-fechamento")
    public ResponseEntity<FaturaRespostaDTO> forcarFechamento(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.forcarFechamento(user, id));
    }

    @PostMapping("/faturas/{id}/pagar")
    public ResponseEntity<TransacaoRespostaDTO> pagar(
            @AuthenticationPrincipal Usuario user,
            @PathVariable Long id,
            @RequestBody @Valid PagarFaturaDTO dto
    ) {
        LocalDate data = dto.data() != null ? dto.data() : LocalDate.now();
        CriarTransacaoDTO tx = new CriarTransacaoDTO(
                TipoTransacao.PAGAMENTO_FATURA,
                dto.valor(),
                "Pagamento da fatura #" + id,
                data,
                dto.contaId(),
                null,
                null,
                null,
                id,
                1,
                null
        );
        TransacaoRespostaDTO resp = transacaoService.criar(user, null, tx);
        return ResponseEntity.ok(resp);
    }
}
