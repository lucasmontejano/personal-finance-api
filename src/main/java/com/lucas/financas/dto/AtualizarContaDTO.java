package com.lucas.financas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// nao deixo trocar tipo nem saldo inicial pra nao bagunçar relatorio depois
public record AtualizarContaDTO(

        @NotBlank
        @Size(max = 120)
        String nome,

        @Size(max = 7)
        String cor
) {
}
