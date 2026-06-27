package com.ehrapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ehrOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Modular EHR API")
                        .description("Modular Electronic Health Record platform. Institutions enable the "
                                + "clinical modules they need; patients consent to share their record "
                                + "across institutions.")
                        .version("1.0.0")
                        .license(new License().name("Proprietary")));
    }
}
