package no.scienta.cfp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the whole programme for the export (programme-export R1-R3). */
@Service
public class ProgrammeExportService {

    private final ProgrammeExportDao dao;

    public ProgrammeExportService(ProgrammeExportDao dao) {
        this.dao = dao;
    }

    /** Everything on the programme now, read in one read-only transaction. */
    @Transactional(readOnly = true)
    public Programme programme() {
        return new Programme(dao.rows());
    }
}
