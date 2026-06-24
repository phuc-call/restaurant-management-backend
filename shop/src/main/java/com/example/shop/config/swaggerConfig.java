package com.example.shop.config;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;

public class  swaggerConfig{
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI().info(new Info().title("E-commerce-Application")
                .description("backend api for E-commerce app")
                .version("v1.0.0")
                .contact(new Contact().name("Hong Phuc").url("hongphuc@hitu.edu.vn").email("phucboygo@gmail.com"))
                .license(new License().name("License").url("/")
                        .url("http://localhost:8080/swagger-ui/index.html"))
        );
    }

}