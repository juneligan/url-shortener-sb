package com.auth.user.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final List<String> frontendUrls;

    public WebConfiguration(@Value("${frontend.urls}") List<String> frontendUrls) {
        this.frontendUrls = Collections.unmodifiableList(frontendUrls);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendUrls.toArray(new String[0]))
                .allowedMethods("*");
    }
}