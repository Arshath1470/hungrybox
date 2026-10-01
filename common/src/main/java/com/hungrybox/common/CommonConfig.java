package com.hungrybox.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungrybox.common.events.EventJson;
import com.hungrybox.common.security.CurrentUserArgumentResolver;
import com.hungrybox.common.security.JwtAuthFilter;
import com.hungrybox.common.security.JwtService;
import com.hungrybox.common.security.SecurityProperties;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Wiring shared by every HungryBox service (picked up via component scan of com.hungrybox). */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class CommonConfig implements WebMvcConfigurer {

    @Bean
    public JwtService jwtService(SecurityProperties properties) {
        return new JwtService(properties);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(JwtService jwtService, SecurityProperties properties) {
        FilterRegistrationBean<JwtAuthFilter> registration =
                new FilterRegistrationBean<>(new JwtAuthFilter(jwtService, properties.publicPaths()));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    public EventJson eventJson(ObjectMapper objectMapper) {
        return new EventJson(objectMapper);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
