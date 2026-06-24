package com.example.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ContentConfig implements WebMvcConfigurer {
    @Override
    public void configureContentNegotiation(@SuppressWarnings("null") ContentNegotiationConfigurer configurer) {
        configurer.favorParameter(true).parameterName("mediaType").defaultContentType(MediaType.APPLICATION_JSON)
                .mediaType("json", MediaType.APPLICATION_JSON).mediaType("xml", MediaType.APPLICATION_XML);
    }
    //allow load fe web create have file in upload
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String externalUploadPath= System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("classpath:/static/uploads/",
                        "file:"+externalUploadPath)
                .setCachePeriod(0);
    }
}
