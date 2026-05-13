package com.lucas.financas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.lucas.financas.TestUtils.cadastrarERetornarToken;
import static com.lucas.financas.TestUtils.comAuth;
import static com.lucas.financas.TestUtils.emailAleatorio;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransacaoIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token;
    private long contaId;
    private long contaDestinoId;
    private long catReceitaId;
    private long catDespesaId;

    @BeforeEach
    void setUp() throws Exception {
        token = cadastrarERetornarToken(mvc, json, emailAleatorio());

        contaId = criarConta("Origem", 1000.00);
        contaDestinoId = criarConta("Destino", 0.00);

        var cats = mvc.perform(comAuth(get("/categorias"), token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode c : json.readTree(cats)) {
            if ("RECEITA".equals(c.get("tipo").asText()) && "Salário".equals(c.get("nome").asText())) {
                catReceitaId = c.get("id").asLong();
            }
            if ("DESPESA".equals(c.get("tipo").asText()) && "Alimentação".equals(c.get("nome").asText())) {
                catDespesaId = c.get("id").asLong();
            }
        }
    }

    @Test
    void receitaDespesaETransferenciaAtualizamSaldo() throws Exception {
        // receita 500 na origem
        criarTx(Map.of(
                "tipo", "RECEITA", "valor", 500.00, "descricao", "extra",
                "data", LocalDate.now().toString(), "contaId", contaId, "categoriaId", catReceitaId
        ));
        // despesa 100 na origem
        criarTx(Map.of(
                "tipo", "DESPESA", "valor", 100.00, "descricao", "mercado",
                "data", LocalDate.now().toString(), "contaId", contaId, "categoriaId", catDespesaId
        ));
        // transfere 200 origem -> destino
        criarTx(Map.of(
                "tipo", "TRANSFERENCIA", "valor", 200.00, "descricao", "reserva",
                "data", LocalDate.now().toString(), "contaId", contaId, "contaDestinoId", contaDestinoId
        ));

        // origem: 1000 + 500 - 100 - 200 = 1200
        mvc.perform(comAuth(get("/contas/" + contaId), token))
                .andExpect(jsonPath("$.saldo").value(1200.0));
        // destino: 0 + 200 = 200
        mvc.perform(comAuth(get("/contas/" + contaDestinoId), token))
                .andExpect(jsonPath("$.saldo").value(200.0));
    }

    @Test
    void idempotencyKeyNaoDuplica() throws Exception {
        String idem = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "tipo", "RECEITA", "valor", 999.00, "descricao", "1a",
                "data", LocalDate.now().toString(), "contaId", contaId, "categoriaId", catReceitaId
        );
        long id1 = postTxComIdem(body, idem);

        Map<String, Object> bodyDif = new HashMap<>(body);
        bodyDif.put("valor", 1.00);
        bodyDif.put("descricao", "2a");
        long id2 = postTxComIdem(bodyDif, idem);

        if (id1 != id2) {
            throw new AssertionError("idempotency falhou: id1=" + id1 + " id2=" + id2);
        }
    }

    @Test
    void categoriaErradaParaTipo() throws Exception {
        var body = Map.of(
                "tipo", "DESPESA", "valor", 10.00, "descricao", "x",
                "data", LocalDate.now().toString(), "contaId", contaId,
                "categoriaId", catReceitaId  // categoria de RECEITA num DESPESA
        );
        mvc.perform(comAuth(post("/transacoes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferenciaParaMesmaContaFalha() throws Exception {
        var body = Map.of(
                "tipo", "TRANSFERENCIA", "valor", 10.00, "descricao", "x",
                "data", LocalDate.now().toString(), "contaId", contaId, "contaDestinoId", contaId
        );
        mvc.perform(comAuth(post("/transacoes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    private long criarConta(String nome, double saldo) throws Exception {
        var body = Map.of("nome", nome, "tipo", "CORRENTE", "saldoInicial", saldo);
        var resp = mvc.perform(comAuth(post("/contas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asLong();
    }

    private void criarTx(Map<String, Object> body) throws Exception {
        mvc.perform(comAuth(post("/transacoes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    private long postTxComIdem(Map<String, Object> body, String idem) throws Exception {
        var resp = mvc.perform(comAuth(post("/transacoes"), token)
                        .header("Idempotency-Key", idem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asLong();
    }
}
