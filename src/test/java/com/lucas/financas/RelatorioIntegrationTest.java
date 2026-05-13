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
class RelatorioIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token;
    private long contaId;
    private long catReceitaId;
    private long catDespesaId;

    @BeforeEach
    void setUp() throws Exception {
        token = cadastrarERetornarToken(mvc, json, emailAleatorio());

        var resp = mvc.perform(comAuth(post("/contas"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nome", "Conta", "tipo", "CORRENTE", "saldoInicial", 0.00))))
                .andReturn().getResponse().getContentAsString();
        contaId = json.readTree(resp).get("id").asLong();

        var cats = mvc.perform(comAuth(get("/categorias"), token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode c : json.readTree(cats)) {
            String nome = c.get("nome").asText();
            String tipo = c.get("tipo").asText();
            if ("RECEITA".equals(tipo) && "Salário".equals(nome)) catReceitaId = c.get("id").asLong();
            if ("DESPESA".equals(tipo) && "Alimentação".equals(nome)) catDespesaId = c.get("id").asLong();
        }
    }

    @Test
    void resumoMensalSomaCertinho() throws Exception {
        LocalDate hoje = LocalDate.now();

        criarTx("RECEITA", 3000.00, catReceitaId, hoje);
        criarTx("DESPESA", 200.00, catDespesaId, hoje);

        mvc.perform(comAuth(get("/relatorios/resumo-mensal?ano=" + hoje.getYear() + "&mes=" + hoje.getMonthValue()), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceitas").value(3000.0))
                .andExpect(jsonPath("$.totalDespesas").value(200.0))
                .andExpect(jsonPath("$.saldoMes").value(2800.0));
    }

    @Test
    void fluxoCaixaForaDoRangeDa400() throws Exception {
        mvc.perform(comAuth(get("/relatorios/fluxo-caixa?meses=99"), token))
                .andExpect(status().isBadRequest());
    }

    private void criarTx(String tipo, double valor, long catId, LocalDate data) throws Exception {
        var body = Map.of(
                "tipo", tipo, "valor", valor, "descricao", "x",
                "data", data.toString(), "contaId", contaId, "categoriaId", catId
        );
        mvc.perform(comAuth(post("/transacoes"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }
}
