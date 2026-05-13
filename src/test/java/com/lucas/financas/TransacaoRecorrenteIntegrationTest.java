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

import java.util.Map;

import static com.lucas.financas.TestUtils.cadastrarERetornarToken;
import static com.lucas.financas.TestUtils.comAuth;
import static com.lucas.financas.TestUtils.emailAleatorio;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransacaoRecorrenteIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token;
    private long contaId;
    private long catDespesaId;

    @BeforeEach
    void setUp() throws Exception {
        token = cadastrarERetornarToken(mvc, json, emailAleatorio());

        var resp = mvc.perform(comAuth(post("/contas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nome", "Conta", "tipo", "CORRENTE", "saldoInicial", 1000.00))))
                .andReturn().getResponse().getContentAsString();
        contaId = json.readTree(resp).get("id").asLong();

        var cats = mvc.perform(comAuth(get("/categorias"), token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode c : json.readTree(cats)) {
            if ("DESPESA".equals(c.get("tipo").asText()) && "Alimentação".equals(c.get("nome").asText())) {
                catDespesaId = c.get("id").asLong();
            }
        }
    }

    @Test
    void executarAgoraGeraTransacaoEEhIdempotente() throws Exception {
        long recId = criarRecorrente(50.00, true);

        var resp1 = mvc.perform(comAuth(post("/transacoes-recorrentes/" + recId + "/executar-agora"), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long txId1 = json.readTree(resp1).get("id").asLong();

        var resp2 = mvc.perform(comAuth(post("/transacoes-recorrentes/" + recId + "/executar-agora"), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long txId2 = json.readTree(resp2).get("id").asLong();

        if (txId1 != txId2) {
            throw new AssertionError("executar-agora duplicou: " + txId1 + " vs " + txId2);
        }

        // saldo deve ter caido 50 só uma vez
        mvc.perform(comAuth(get("/contas/" + contaId), token))
                .andExpect(jsonPath("$.saldo").value(950.0));
    }

    @Test
    void executarAgoraEmRecorrentePausadaDa400() throws Exception {
        long recId = criarRecorrente(10.00, false);
        mvc.perform(comAuth(post("/transacoes-recorrentes/" + recId + "/executar-agora"), token))
                .andExpect(status().isBadRequest());
    }

    private long criarRecorrente(double valor, boolean ativa) throws Exception {
        var body = Map.of(
                "tipo", "DESPESA", "valor", valor, "descricao", "Internet",
                "contaId", contaId, "categoriaId", catDespesaId,
                "diaDoMes", 15, "dataInicio", "2020-01-01"
        );
        var resp = mvc.perform(comAuth(post("/transacoes-recorrentes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resp).get("id").asLong();

        if (!ativa) {
            var update = Map.of(
                    "tipo", "DESPESA", "valor", valor, "descricao", "Internet",
                    "contaId", contaId, "categoriaId", catDespesaId,
                    "diaDoMes", 15, "dataInicio", "2020-01-01",
                    "ativa", false
            );
            mvc.perform(comAuth(put("/transacoes-recorrentes/" + id), token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(update)))
                    .andExpect(status().isOk());
        }
        return id;
    }
}
