package no.scienta.cfp;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP API of the programme export: the JSON list for the website and the CSV file for the print shop
 * (programme-export R1, R5). The path chooses the view: each answers in its own type, whatever the client's
 * {@code Accept} header asks for, as decided in workflow/specs/programme-export/spec.md.
 */
@RestController
public class ProgrammeExportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final ProgrammeExportService service;

    public ProgrammeExportController(ProgrammeExportService service) {
        this.service = service;
    }

    @GetMapping("/programme")
    public ResponseEntity<List<ProgrammeRow>> programme() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.programme().rows());
    }

    @GetMapping("/programme.csv")
    public ResponseEntity<String> programmeCsv() {
        return ResponseEntity.ok().contentType(CSV).body(ProgrammeCsv.of(service.programme()));
    }
}
