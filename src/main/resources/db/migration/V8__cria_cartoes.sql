CREATE TABLE cartoes (
    id                            BIGSERIAL PRIMARY KEY,
    usuario_id                    BIGINT NOT NULL REFERENCES usuarios(id),
    nome                          VARCHAR(120) NOT NULL,
    bandeira                      VARCHAR(30),
    limite                        NUMERIC(15,2) NOT NULL CHECK (limite >= 0),
    dia_fechamento                INTEGER NOT NULL CHECK (dia_fechamento BETWEEN 1 AND 31),
    dia_vencimento                INTEGER NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 31),
    conta_padrao_pagamento_id     BIGINT REFERENCES contas(id),
    cor                           VARCHAR(7),
    ativo                         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em                     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cartoes_usuario ON cartoes(usuario_id);
