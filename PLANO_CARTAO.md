# Plano — Cartão de crédito (Fase 8) + roadmap

Arquivo temporário. Deletar quando a Fase 8 estiver completa.

## Decisões de modelagem

1. **Cartão é entidade separada de Conta** (não TipoConta novo). Vai pra `/cartoes`. Tem `limite`, não `saldoInicial`.
2. **Despesa no cartão reusa `Transacao`** com `cartao_id` e `fatura_id` nullable. Quando `cartao_id` presente, `conta_id` é NULL e `ContaService.calcularSaldo` ignora a linha.
3. **Parcelamento = N transações + `compra_parcelada_id` (UUID)** com `numero_parcela`/`total_parcelas`. Cancelar = soft-delete cascade pelo UUID.
4. **Pagamento de fatura = novo tipo `PAGAMENTO_FATURA`**. Tem `conta_id` (origem) + `fatura_id` (destino). Sem categoria.
5. **Faturas criadas lazy** ao inserir transação de cartão. Scheduler diário só transiciona ABERTA→FECHADA.
6. **Validação tipo×cartão** estende a regra atual: DESPESA/RECEITA aceita contaId XOR cartaoId; PAGAMENTO_FATURA exige contaId+faturaId.

## Schema (V8, V9, V10)

**V8 cartoes**: id, usuario_id, nome, bandeira?, limite, dia_fechamento(1-31), dia_vencimento(1-31), conta_padrao_pagamento_id?, ativo, criado_em.

**V9 faturas**: id, cartao_id, mes_referencia(DATE dia 1), data_fechamento, data_vencimento, status(ABERTA|FECHADA|PAGA), valor_pago. UNIQUE(cartao_id, mes_referencia).

**V10 altera transacoes**: adiciona cartao_id, fatura_id, compra_parcelada_id(UUID), numero_parcela, total_parcelas. Atualiza CHECK do tipo pra incluir PAGAMENTO_FATURA. Índices em cartao_id e fatura_id.

## Endpoints

```
POST   /cartoes
GET    /cartoes
GET    /cartoes/{id}                            inclui limite_usado e limite_disponivel
PUT    /cartoes/{id}
DELETE /cartoes/{id}                            soft-delete; 409 se tem fatura nao paga

GET    /cartoes/{id}/faturas                    paginado, filtro status
GET    /cartoes/{id}/faturas/atual              fatura ABERTA atual
GET    /faturas/{id}                            detalhe + transacoes
POST   /faturas/{id}/pagar                      {contaId, valor} -> cria PAGAMENTO_FATURA

POST   /transacoes                              ja existe; agora aceita cartaoId + parcelas
```

## Sub-fases

- **8a** CRUD Cartao (V8 + entity/DTO/repo/service/controller + smoke)
- **8b** Faturas read-only (V9 + entity + findOuCriaPorData + GET endpoints + smoke)
- **8c** Compras no cartao + parcelamento (V10 + adapta Transacao + validacao + ajusta calcularSaldo + smoke)
- **8d** Pagamento de fatura (POST /faturas/{id}/pagar + tipo PAGAMENTO_FATURA + smoke)
- **8e** Scheduler de fechamento (cron diario ABERTA->FECHADA + smoke simulando data)

## Edge cases

- Soft-delete de cartao com fatura nao paga -> 409
- Update de transacao de cartao: permite valor/desc/cat; proibe data/cartao
- Estorno = RECEITA com cartao_id (reduz valor da fatura)
- Limite estourado: so avisa, nao bloqueia
- Fora de escopo: cotacao internacional, juros por atraso, antecipacao

## Roadmap pos-cartao

- **9** Dashboard agregado (`GET /dashboard`)
- **10** Importacao OFX/CSV
- **11** Metas/poupanca
- **12** Anexos (foto da nota)
- **13** Exportacao CSV/PDF + backup JSON
- **14** Refresh token + logout
- **15** Deploy (Dockerfile multi-stage + compose unificado)
