package com.aitutor.backend.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OwnershipWebConfiguration implements WebMvcConfigurer {
    private final OwnershipInterceptor interceptor;

    public OwnershipWebConfiguration(OwnershipInterceptor interceptor) { this.interceptor = interceptor; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/**").excludePathPatterns(
                "/api/v1/health", "/api/v1/auth/register");
    }
}
