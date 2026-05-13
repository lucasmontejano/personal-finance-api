package com.lucas.financas.controller;

import com.lucas.financas.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// endpoint pra testar se o JWT ta funcionando
@RestController
@RequestMapping("/me")
public class MeController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Usuario user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "nome", user.getNome(),
                "email", user.getEmail()
        ));
    }
}
