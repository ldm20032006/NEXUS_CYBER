package demo.server.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI nexusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("NEXUS Smart Cyber Esports API")
                        .version("0.0.1")
                        .description("Foundation API documentation for the NEXUS modular monolith."));
    }
}
