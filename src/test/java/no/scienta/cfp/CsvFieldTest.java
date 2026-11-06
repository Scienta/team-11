package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.tabletest.junit.Description;
import org.tabletest.junit.TableTest;
import org.tabletest.junit.TypeConverter;

/**
 * The values of the CSV export in {@link CsvField}.
 */
public class CsvFieldTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(value = {
            "Plain title           | Plain title",
            "Semi; colon           | \"Semi; colon\"",
            "Say \"hi\"            | \"Say 'hi'\"",
    }, delimiter = '|')
    void quotesATextThatHoldsASemicolonOrAQuote(String value, String field) {
        assertEquals(field, CsvField.text(value));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(value = {
            "WORKSHOP     | Workshop",
            "LIGHTNING 10 | Lightning 10",
            "'   '        | '   '",
    }, delimiter = '|')
    void capitalizesTheFirstLetterOnly(String value, String field) {
        assertEquals(field, CsvField.capitalized(value));
    }

    @DisplayName("Exports a value that starts like a formula as text")
    @Description("""
            A spreadsheet runs a cell that starts with =, +, - or @ as a formula, also after a tab. The
            export of /admin/talks.csv puts ' in front, as the OWASP advice on CSV injection says. A
            title such as "-1 tips" also gets the ', because the export cannot tell it from a formula.
            In a cell, \\t is a tab.
            """)
    @TableTest("""
        Scenario                     | Value           | Exported?
        Starts with =                | =1+1            | "'=1+1"
        Starts with +                | +47 22 33 44 55 | "'+47 22 33 44 55"
        Starts with -                | -1 tips         | "'-1 tips"
        Starts with @                | @SUM(A1)        | "'@SUM(A1)"
        Starts with a tab            | \\t=1+1         | "'\\t=1+1"
        Formula sign after the start | 1+1=2           | 1+1=2
        """)
    void exportsAValueThatStartsLikeAFormulaAsText(CsvText value, CsvText exported) {
        assertEquals(exported.value(), CsvField.text(value.value()));
    }

    @DisplayName("Quotes a value with a semicolon or a line break")
    @Description("""
            The export separates the columns with ; and the talks with a line break. In a cell, \\n is a
            line feed and \\r is a carriage return.
            """)
    @TableTest("""
        Scenario        | Value                    | Exported?
        Plain text      | Legacy is a feeling      | Legacy is a feeling
        Semicolon       | Legacy; a feeling        | '"Legacy; a feeling"'
        Line feed       | Legacy\\nis a feeling    | '"Legacy\\nis a feeling"'
        Carriage return | Legacy\\r\\nis a feeling | '"Legacy\\r\\nis a feeling"'
        """)
    void quotesAValueWithASemicolonOrALineBreak(CsvText value, CsvText exported) {
        assertEquals(exported.value(), CsvField.text(value.value()));
    }

    @Test
    void putsTheFormulaMarkInsideTheCsvQuotes() {
        assertEquals("\"'=1;2\"", CsvField.text("=1;2"));
        assertEquals("\"'\r=1+1\"", CsvField.text("\r=1+1"));
    }

    /** A CSV value as a table cell writes it: \\t, \\r and \\n stand for a tab, a carriage return and a line feed. */
    @TypeConverter
    public static CsvText parseCsvText(String cell) {
        return new CsvText(cell.replace("\\t", "\t").replace("\\r", "\r").replace("\\n", "\n"));
    }

    record CsvText(String value) {
    }
}
