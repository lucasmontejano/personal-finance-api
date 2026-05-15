-- adiciona suporte a cartao + parcelamento nas transacoes
ALTER TABLE transacoes
    ADD COLUMN cartao_id              BIGINT REFERENCES cartoes(id),
    ADD COLUMN fatura_id              BIGINT REFERENCES faturas(id),
    ADD COLUMN compra_parcelada_id    UUID,
    ADD COLUMN numero_parcela         INTEGER,
    ADD COLUMN total_parcelas         INTEGER;

-- conta_id passa a aceitar NULL (transacao de cartao nao tem conta)
ALTER TABLE transacoes ALTER COLUMN conta_id DROP NOT NULL;

-- check pra impedir tipo invalido depois do enum ganhar PAGAMENTO_FATURA
ALTER TABLE transacoes ADD CONSTRAINT ck_transacoes_tipo
    CHECK (tipo IN ('RECEITA','DESPESA','TRANSFERENCIA','PAGAMENTO_FATURA'));

CREATE INDEX idx_transacoes_cartao           ON transacoes(cartao_id);
CREATE INDEX idx_transacoes_fatura           ON transacoes(fatura_id);
CREATE INDEX idx_transacoes_compra_parcelada ON transacoes(compra_parcelada_id);
