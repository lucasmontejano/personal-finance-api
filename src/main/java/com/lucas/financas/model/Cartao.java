package com.lucas.financas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cartoes")
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 30)
    private String bandeira;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limite;

    @Column(name = "dia_fechamento", nullable = false)
    private Integer diaFechamento;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    // conta padrao pra debitar pagamento da fatura (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_padrao_pagamento_id")
    private Conta contaPadraoPagamento;

    @Column(length = 7)
    private String cor;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public Cartao() {
    }

    public Cartao(
            Usuario usuario,
            String nome,
            String bandeira,
            BigDecimal limite,
            Integer diaFechamento,
            Integer diaVencimento,
            Conta contaPadraoPagamento,
            String cor
    ) {
        this.usuario = usuario;
        this.nome = nome;
        this.bandeira = bandeira;
        this.limite = limite;
        this.diaFechamento = diaFechamento;
        this.diaVencimento = diaVencimento;
        this.contaPadraoPagamento = contaPadraoPagamento;
        this.cor = cor;
        this.ativo = true;
    }

    @PrePersist
    void prePersist() {
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }

    public BigDecimal getLimite() {
        return limite;
    }

    public void setLimite(BigDecimal limite) {
        this.limite = limite;
    }

    public Integer getDiaFechamento() {
        return diaFechamento;
    }

    public void setDiaFechamento(Integer diaFechamento) {
        this.diaFechamento = diaFechamento;
    }

    public Integer getDiaVencimento() {
        return diaVencimento;
    }

    public void setDiaVencimento(Integer diaVencimento) {
        this.diaVencimento = diaVencimento;
    }

    public Conta getContaPadraoPagamento() {
        return contaPadraoPagamento;
    }

    public void setContaPadraoPagamento(Conta contaPadraoPagamento) {
        this.contaPadraoPagamento = contaPadraoPagamento;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
