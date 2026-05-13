package com.lucas.financas;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void cadastraEAcessaMe() throws Exception {
        String email = emailAleatorio();
        String token = cadastrarERetornarToken(mvc, json, email);

        mvc.perform(comAuth(get("/me"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void loginRetornaToken() throws Exception {
        String email = emailAleatorio();
        cadastrarERetornarToken(mvc, json, email);

        var body = Map.of("email", email, "senha", "senha123");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void loginSenhaErrada() throws Exception {
        String email = emailAleatorio();
        cadastrarERetornarToken(mvc, json, email);

        var body = Map.of("email", email, "senha", "errada");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroEmailDuplicado() throws Exception {
        String email = emailAleatorio();
        cadastrarERetornarToken(mvc, json, email);

        var body = Map.of("nome", "Outro", "email", email, "senha", "senha123");
        mvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meSemTokenBarra() throws Exception {
        mvc.perform(get("/me"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    if (s != 401 && s != 403) throw new AssertionError("esperado 401/403, veio " + s);
                });
    }
}
