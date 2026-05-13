CREATE TABLE transacoes (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id),
    conta_id            BIGINT NOT NULL REFERENCES contas(id),
    conta_destino_id    BIGINT REFERENCES contas(id),
    categoria_id        BIGINT REFERENCES categorias(id),
    tipo                VARCHAR(20) NOT NULL,
    valor               NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    descricao           VARCHAR(255) NOT NULL,
    data                DATE NOT NULL,
    observacoes         TEXT,
    idempotency_key     VARCHAR(80),
    deletado            BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transacoes_usuario ON transacoes(usuario_id);
CREATE INDEX idx_transacoes_conta ON transacoes(conta_id);
CREATE INDEX idx_transacoes_data ON transacoes(data);

-- partial unique pra idempotencia (ignora nulos)
CREATE UNIQUE INDEX idx_transacoes_idempotency
    ON transacoes(usuario_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
