package no.scienta.cfp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.UrlHandlerFilter;

/**
 * Speakers R7: {@code /speakers/} and {@code /speakers/{id}/} answer like the paths without the slash, and so does
 * every other path, as on Spring Boot 2.7. Spring 6+ no longer matches a trailing slash, so this filter strips it
 * before the controllers see the request. It wraps the request rather than redirecting, so a POST or PUT keeps its
 * body and its status.
 */
@Configuration
class TrailingSlashConfig {

    @Bean
    UrlHandlerFilter trailingSlashFilter() {
        return UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build();
    }
}
