package com.lucas.financas.dto;

import com.lucas.financas.model.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarCategoriaDTO(

        @NotBlank(message = "nome é obrigatório")
        @Size(max = 80)
        String nome,

        @NotNull(message = "tipo é obrigatório")
        TipoCategoria tipo,

        @Size(max = 40)
        String icone,

        @Size(max = 7)
        String cor
) {
}
