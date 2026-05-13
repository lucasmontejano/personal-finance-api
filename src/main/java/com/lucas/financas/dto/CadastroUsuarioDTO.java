package com.lucas.financas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroUsuarioDTO(

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 120)
        String nome,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,

        @NotBlank
        @Size(min = 6, max = 100, message = "senha precisa ter no minimo 6 caracteres")
        String senha
) {
}
