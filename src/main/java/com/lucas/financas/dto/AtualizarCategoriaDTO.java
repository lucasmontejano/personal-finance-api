package com.lucas.financas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// nao deixo trocar tipo - pra nao quebrar transação associada depois
public record AtualizarCategoriaDTO(

        @NotBlank
        @Size(max = 80)
        String nome,

        @Size(max = 40)
        String icone,

        @Size(max = 7)
        String cor
) {
}
