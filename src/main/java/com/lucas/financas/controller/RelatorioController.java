package com.lucas.financas.controller;

import com.lucas.financas.dto.FluxoCaixaDTO;
import com.lucas.financas.dto.RelatorioPorCategoriaDTO;
import com.lucas.financas.dto.ResumoMensalDTO;
import com.lucas.financas.model.TipoCategoria;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/resumo-mensal")
    public ResponseEntity<ResumoMensalDTO> resumoMensal(
            @AuthenticationPrincipal Usuario user,
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        return ResponseEntity.ok(service.resumoMensal(user, ano, mes));
    }

    @GetMapping("/por-categoria")
    public ResponseEntity<RelatorioPorCategoriaDTO> porCategoria(
            @AuthenticationPrincipal Usuario user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "DESPESA") TipoCategoria tipo
    ) {
        return ResponseEntity.ok(service.porCategoria(user, inicio, fim, tipo));
    }

    @GetMapping("/fluxo-caixa")
    public ResponseEntity<FluxoCaixaDTO> fluxoCaixa(
            @AuthenticationPrincipal Usuario user,
            @RequestParam(defaultValue = "6") int meses
    ) {
        return ResponseEntity.ok(service.fluxoCaixa(user, meses));
    }
}
