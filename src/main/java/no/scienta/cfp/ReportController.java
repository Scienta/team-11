package no.scienta.cfp;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exports for the programme committee's spreadsheet. Not part of the public API.
 */
@RestController
public class ReportController {

    private final ConferenceStatistics statistics;
    private final TalkCsvExport csvExport;

    public ReportController(ConferenceStatistics statistics, TalkCsvExport csvExport) {
        this.statistics = statistics;
        this.csvExport = csvExport;
    }

    @GetMapping("/admin/stats")
    public Map<String, Object> stats() {
        return statistics.statistics();
    }

    @GetMapping("/admin/talks.csv")
    public ResponseEntity<String> talksCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=talks.csv")
                .body(csvExport.csv());
    }
}
