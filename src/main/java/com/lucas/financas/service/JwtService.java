package com.lucas.financas.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long validadeMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiracao-horas}") long expiracaoHoras
    ) {
        this.chave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.validadeMillis = expiracaoHoras * 60 * 60 * 1000;
    }

    public String gerar(String email) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + validadeMillis))
                .signWith(chave)
                .compact();
    }

    public String extrairEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean valido(String token) {
        try {
            Date exp = getClaims(token).getExpiration();
            return exp.after(new Date());
        } catch (Exception e) {
            // token invalido, expirado, assinatura errada, etc
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
