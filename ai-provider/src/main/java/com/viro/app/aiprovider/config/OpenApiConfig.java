package com.viro.app.aiprovider.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration - Industry Standard Pattern
 *
 * Configures Swagger/OpenAPI documentation for the AI API.
 * Accessible at: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aiProviderOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Viro AI Provider API")
                .description("""
                    AI-powered operations including text chat, document analysis, and image understanding.

                    ## Features
                    - Text chat with multiple AI providers (OpenAI, Anthropic)
                    - Document analysis (PDFs, images, text files)
                    - Image understanding and description
                    - Function/tool calling for dynamic data access

                    ## Providers
                    - **openai**: GPT-4o-mini (text), GPT-4o (vision)
                    - **anthropic**: Claude 3.5 Sonnet (text & vision)

                    ## For n8n Integration
                    Use the HTTP Request node to call these endpoints.
                    All endpoints support both JSON and form-data requests.
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Viro Support")
                    .email("support@viro.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development"),
                new Server()
                    .url("https://api.viro.com")
                    .description("Production")
            ));
    }
}
