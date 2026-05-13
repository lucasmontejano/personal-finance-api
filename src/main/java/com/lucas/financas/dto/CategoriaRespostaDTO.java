package com.lucas.financas.dto;

import com.lucas.financas.model.Categoria;
import com.lucas.financas.model.TipoCategoria;

public record CategoriaRespostaDTO(
        Long id,
        String nome,
        TipoCategoria tipo,
        String icone,
        String cor
) {

    public static CategoriaRespostaDTO de(Categoria c) {
        return new CategoriaRespostaDTO(
                c.getId(),
                c.getNome(),
                c.getTipo(),
                c.getIcone(),
                c.getCor()
        );
    }
}
