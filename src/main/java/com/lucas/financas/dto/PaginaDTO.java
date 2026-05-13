package com.lucas.financas.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// envelope simples pra paginacao - evita expor o formato do Page do Spring
public record PaginaDTO<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long total,
        int totalPaginas
) {

    public static <T> PaginaDTO<T> de(Page<T> p) {
        return new PaginaDTO<>(
                p.getContent(),
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages()
        );
    }
}
