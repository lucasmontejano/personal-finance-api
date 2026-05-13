package com.lucas.financas.dto;

public record TokenRespostaDTO(String token, String tipo) {

    public static TokenRespostaDTO bearer(String token) {
        return new TokenRespostaDTO(token, "Bearer");
    }
}
