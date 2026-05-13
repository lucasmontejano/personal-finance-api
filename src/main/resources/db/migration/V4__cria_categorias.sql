CREATE TABLE categorias (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id),
    nome        VARCHAR(80) NOT NULL,
    tipo        VARCHAR(15) NOT NULL,
    icone       VARCHAR(40),
    cor         VARCHAR(7),
    criado_em   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, nome, tipo)
);

CREATE INDEX idx_categorias_usuario ON categorias(usuario_id);
