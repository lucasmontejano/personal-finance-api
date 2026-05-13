CREATE TABLE transacoes_recorrentes (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id),
    conta_id            BIGINT NOT NULL REFERENCES contas(id),
    conta_destino_id    BIGINT REFERENCES contas(id),
    categoria_id        BIGINT REFERENCES categorias(id),
    tipo                VARCHAR(20) NOT NULL,
    valor               NUMERIC(15,2) NOT NULL CHECK (valor > 0),
    descricao           VARCHAR(255) NOT NULL,
    dia_do_mes          SMALLINT NOT NULL CHECK (dia_do_mes BETWEEN 1 AND 31),
    data_inicio         DATE NOT NULL,
    data_fim            DATE,
    ativa               BOOLEAN NOT NULL DEFAULT TRUE,
    ultima_execucao     DATE,
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recorrentes_usuario ON transacoes_recorrentes(usuario_id);
CREATE INDEX idx_recorrentes_ativas ON transacoes_recorrentes(ativa) WHERE ativa = TRUE;
