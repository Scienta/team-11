package no.scienta.cfp;

import java.sql.Date;

import org.springframework.stereotype.Component;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Writes a {@link java.sql.Date} as {@code yyyy-MM-dd}, its {@code toString()}, as Jackson 2 did for
 * {@code submittedOn}. Jackson 3 writes it as a full timestamp, even with Boot's Jackson 2 defaults.
 */
@Component
class SqlDateJsonModule extends SimpleModule {

    SqlDateJsonModule() {
        super("sql-date-as-plain-date");
        addSerializer(Date.class, ToStringSerializer.instance);
    }
}
