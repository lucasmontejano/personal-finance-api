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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ContaIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = cadastrarERetornarToken(mvc, json, emailAleatorio());
    }

    @Test
    void criaEListaConta() throws Exception {
        var body = Map.of("nome", "Nubank", "tipo", "CORRENTE", "saldoInicial", 1000.00);

        var resp = mvc.perform(comAuth(post("/contas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Nubank"))
                .andReturn().getResponse().getContentAsString();

        long id = json.readTree(resp).get("id").asLong();

        mvc.perform(comAuth(get("/contas/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(1000.0));
    }

    @Test
    void deleteSoftSomeDaListagem() throws Exception {
        var body = Map.of("nome", "Vai Sumir", "tipo", "POUPANCA", "saldoInicial", 0.00);
        var resp = mvc.perform(comAuth(post("/contas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resp).get("id").asLong();

        mvc.perform(comAuth(delete("/contas/" + id), token))
                .andExpect(status().isNoContent());

        var lista = mvc.perform(comAuth(get("/contas"), token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode n : json.readTree(lista)) {
            if (n.get("id").asLong() == id) {
                throw new AssertionError("conta soft-deletada apareceu na lista");
            }
        }
    }

    @Test
    void contaDeOutroUsuarioRetorna404() throws Exception {
        var body = Map.of("nome", "Minha", "tipo", "CORRENTE", "saldoInicial", 100.00);
        var resp = mvc.perform(comAuth(post("/contas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resp).get("id").asLong();

        String tokenOutro = cadastrarERetornarToken(mvc, json, emailAleatorio());
        mvc.perform(comAuth(get("/contas/" + id), tokenOutro))
                .andExpect(status().isNotFound());
    }
}
