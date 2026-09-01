package br.com.docintelligence;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação OpenAPI, servida como Swagger UI em
 * {@code /swagger-ui.html} pelo springdoc (sem configuração adicional).
 *
 * <p>O detalhamento de cada operação — descrição, códigos de erro e exemplos de
 * corpo — está nas anotações do {@link br.com.docintelligence.documento.DocumentoController}.
 * O contrato de referência é {@code docs/01-especificacao.md} §4.
 */
@Configuration
public class ConfiguracaoOpenApi {

    @Bean
    OpenAPI documentacaoDocIntelligence() {
        return new OpenAPI().info(new Info()
                .title("DOC Intelligence")
                .description("API da fatia vertical do serviço de inteligência documental; "
                        + "o contrato completo está em docs/01-especificacao.md §4.")
                .version("0.0.1-SNAPSHOT"));
    }
}
