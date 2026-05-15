CREATE TABLE faturas (
    id                  BIGSERIAL PRIMARY KEY,
    cartao_id           BIGINT NOT NULL REFERENCES cartoes(id),
    mes_referencia      DATE NOT NULL,
    data_fechamento     DATE NOT NULL,
    data_vencimento     DATE NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    valor_pago          NUMERIC(15,2) NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_faturas_cartao_mes UNIQUE (cartao_id, mes_referencia),
    CONSTRAINT ck_faturas_status CHECK (status IN ('ABERTA','FECHADA','PAGA'))
);

CREATE INDEX idx_faturas_cartao ON faturas(cartao_id);
CREATE INDEX idx_faturas_status ON faturas(status);
