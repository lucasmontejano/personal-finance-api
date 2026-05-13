package com.lucas.financas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI financasOpenAPI() {
        SecurityScheme jwt = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("API Financas")
                        .description("API pra gerenciar financas pessoais — contas, transacoes, orçamentos e relatorios.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER, jwt))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
