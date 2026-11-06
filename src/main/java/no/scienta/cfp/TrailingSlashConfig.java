package no.scienta.cfp;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.UrlHandlerFilter;

/**
 * Speakers R7: {@code /speakers/} and {@code /speakers/{id}/} answer like the paths without the slash, and so does
 * every other path, as on Spring Boot 2.7. Spring 6+ no longer matches a trailing slash, so this filter strips it
 * before the controllers see the request. It wraps the request rather than redirecting, so a POST or PUT keeps its
 * body and its status. It runs before {@link RequestLogFilter} and {@link RequestBodyLimitFilter}, so their order
 * between them stays as it is and the body limit applies to a path with a slash too.
 */
@Configuration
class TrailingSlashConfig {

    static final int ORDER = Ordered.LOWEST_PRECEDENCE - 2;

    @Bean
    FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        FilterRegistrationBean<UrlHandlerFilter> registration =
                new FilterRegistrationBean<>(UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build());
        registration.setOrder(ORDER);
        return registration;
    }
}
