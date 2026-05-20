package com.aihealthcoach.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI aiHealthCoachOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Health Coach API")
                        .description("AI Health Coach 백엔드 API 문서")
                        .version("v1"));
    }
}
