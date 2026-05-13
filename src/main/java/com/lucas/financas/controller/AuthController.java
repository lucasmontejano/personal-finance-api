package com.lucas.financas.controller;

import com.lucas.financas.dto.CadastroUsuarioDTO;
import com.lucas.financas.dto.LoginDTO;
import com.lucas.financas.dto.TokenRespostaDTO;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.service.JwtService;
import com.lucas.financas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    public AuthController(
            UsuarioService usuarioService,
            AuthenticationManager authManager,
            JwtService jwtService
    ) {
        this.usuarioService = usuarioService;
        this.authManager = authManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<TokenRespostaDTO> cadastro(@RequestBody @Valid CadastroUsuarioDTO dto) {
        Usuario u = usuarioService.cadastrar(dto);
        // ja devolve o token pra nao precisar logar logo apos cadastrar
        String token = jwtService.gerar(u.getEmail());
        return ResponseEntity.ok(TokenRespostaDTO.bearer(token));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenRespostaDTO> login(@RequestBody @Valid LoginDTO dto) {
        // se a senha estiver errada, o authenticate lanca BadCredentialsException
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
        );
        String token = jwtService.gerar(dto.email());
        return ResponseEntity.ok(TokenRespostaDTO.bearer(token));
    }
}
