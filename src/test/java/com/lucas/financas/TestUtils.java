package com.lucas.financas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// helpers pros testes de integracao — evita repetir cadastro/json toda hora
public final class TestUtils {

    private TestUtils() {
    }

    public static String emailAleatorio() {
        return "teste_" + UUID.randomUUID().toString().substring(0, 8) + "@teste.com";
    }

    public static String cadastrarERetornarToken(MockMvc mvc, ObjectMapper json, String email) throws Exception {
        var body = Map.of("nome", "Teste", "email", email, "senha", "senha123");
        var resp = mvc.perform(post("/auth/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    public static MockHttpServletRequestBuilder comAuth(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }

    public static JsonNode parse(ObjectMapper json, String body) throws Exception {
        return json.readTree(body);
    }
}
