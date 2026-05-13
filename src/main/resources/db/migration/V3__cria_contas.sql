CREATE TABLE contas (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id),
    nome            VARCHAR(120) NOT NULL,
    tipo            VARCHAR(30) NOT NULL,
    saldo_inicial   NUMERIC(15,2) NOT NULL DEFAULT 0,
    cor             VARCHAR(7),
    ativa           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contas_usuario ON contas(usuario_id);
