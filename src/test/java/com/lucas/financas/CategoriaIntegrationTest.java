package com.lucas.financas;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoriaIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = cadastrarERetornarToken(mvc, json, emailAleatorio());
    }

    @Test
    void cadastroSeedaCategorias() throws Exception {
        // user novo deve receber as 13 categorias padrao
        mvc.perform(comAuth(get("/categorias"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13));
    }

    @Test
    void criaCategoriaCustomizada() throws Exception {
        var body = Map.of("nome", "Pets", "tipo", "DESPESA", "icone", "pet", "cor", "#FF0000");
        mvc.perform(comAuth(post("/categorias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Pets"));
    }

    @Test
    void naoDeixaDuplicarMesmoNomeETipo() throws Exception {
        // "Salário" / RECEITA ja vem do seed
        var body = Map.of("nome", "Salário", "tipo", "RECEITA");
        mvc.perform(comAuth(post("/categorias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
