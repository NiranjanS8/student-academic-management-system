package com.example.sams.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI studentAcademicManagementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Student Academic Management System API")
                .description("Backend APIs for managing academic operations, enrollment, attendance, grading, and fees.")
                .version("v1")
                .contact(new Contact().name("Student Academic Management System")));
    }
}
