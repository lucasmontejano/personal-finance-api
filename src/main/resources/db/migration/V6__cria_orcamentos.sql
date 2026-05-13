CREATE TABLE orcamentos (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT NOT NULL REFERENCES usuarios(id),
    categoria_id    BIGINT NOT NULL REFERENCES categorias(id),
    mes_referencia  DATE NOT NULL,
    valor_limite    NUMERIC(15,2) NOT NULL CHECK (valor_limite > 0),
    criado_em       TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, categoria_id, mes_referencia)
);

CREATE INDEX idx_orcamentos_usuario ON orcamentos(usuario_id);
CREATE INDEX idx_orcamentos_mes ON orcamentos(mes_referencia);
