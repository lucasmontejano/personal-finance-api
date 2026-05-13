package com.lucas.financas.service;

import com.lucas.financas.dto.CadastroUsuarioDTO;
import com.lucas.financas.model.Usuario;
import com.lucas.financas.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final CategoriaService categoriaService;

    public UsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            CategoriaService categoriaService
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.categoriaService = categoriaService;
    }

    @Transactional
    public Usuario cadastrar(CadastroUsuarioDTO dto) {
        if (repository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("email ja cadastrado");
        }

        Usuario u = new Usuario(
                dto.nome(),
                dto.email(),
                passwordEncoder.encode(dto.senha())
        );
        Usuario salvo = repository.save(u);

        // ja cria as categorias padrao pra ele
        categoriaService.seedPadrao(salvo);

        return salvo;
    }
}
