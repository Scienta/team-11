package no.scienta.cfp;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Error bodies are {@code application/json;charset=UTF-8}, like successes, as on Spring Boot 2.7. Spring 7 resets the
 * response before the error page, which drops the UTF-8 that {@code spring.servlet.encoding.force} set, so this
 * filter sets it again on the error dispatch only.
 */
@Configuration
class ErrorCharsetConfig {

    @Bean
    FilterRegistrationBean<Filter> errorCharsetFilter() {
        Filter utf8 = (request, response, chain) -> {
            response.setCharacterEncoding(StandardCharsets.UTF_8);
            chain.doFilter(request, response);
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(utf8);
        registration.setDispatcherTypes(DispatcherType.ERROR);
        return registration;
    }
}
