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
import java.util.Map;

import static com.lucas.financas.TestUtils.cadastrarERetornarToken;
import static com.lucas.financas.TestUtils.comAuth;
import static com.lucas.financas.TestUtils.emailAleatorio;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrcamentoIntegrationTest extends BaseIntegrationTest {

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
    void comparativoMostraGastoEStatus() throws Exception {
        LocalDate hoje = LocalDate.now();

        // gera uma despesa de 80 reais
        var despesa = Map.of(
                "tipo", "DESPESA", "valor", 80.00, "descricao", "lanche",
                "data", hoje.toString(), "contaId", contaId, "categoriaId", catDespesaId
        );
        mvc.perform(comAuth(post("/transacoes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(despesa)))
                .andExpect(status().isCreated());

        // orçamento de 100 — deve ficar em ATENCAO (>=80%)
        var orc = Map.of("categoriaId", catDespesaId, "ano", hoje.getYear(), "mes", hoje.getMonthValue(), "valorLimite", 100.00);
        mvc.perform(comAuth(post("/orcamentos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(orc)))
                .andExpect(status().isCreated());

        mvc.perform(comAuth(get("/orcamentos/comparativo?ano=" + hoje.getYear() + "&mes=" + hoje.getMonthValue()), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].gasto").value(80.0))
                .andExpect(jsonPath("$.itens[0].status").value("ATENCAO"));
    }

    @Test
    void naoPermiteDuplicarOrcamentoNoMesmoMes() throws Exception {
        LocalDate hoje = LocalDate.now();
        var orc = Map.of("categoriaId", catDespesaId, "ano", hoje.getYear(), "mes", hoje.getMonthValue(), "valorLimite", 500.00);

        mvc.perform(comAuth(post("/orcamentos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(orc)))
                .andExpect(status().isCreated());

        mvc.perform(comAuth(post("/orcamentos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(orc)))
                .andExpect(status().isBadRequest());
    }
}
