package no.scienta.cfp;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * All talks as semicolon-separated values for the programme committee's spreadsheet.
 */
@Component
public class TalkCsvExport {

    private final TalkService talkService;
    private final SpeakerDao speakerDao;

    public TalkCsvExport(TalkService talkService, SpeakerDao speakerDao) {
        this.talkService = talkService;
        this.speakerDao = speakerDao;
    }

    @Transactional(readOnly = true)
    public String csv() {
        List<TalkResponse> talks = talkService.listTalks(TalkListQuery.EVERY_TALK);
        Map<Long, Speaker> speakersById = speakerDao.findAll().stream()
                .collect(Collectors.toMap(Speaker::id, Function.identity()));
        StringBuilder sb = new StringBuilder();
        sb.append("id;title;format;minutes;level;speaker;status;submitted;rating;ratings\n");
        for (TalkResponse t : talks) {
            Speaker speaker = Optional.ofNullable(speakersById.get(t.speakerId()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "speaker not found"));
            sb.append(t.id()).append(';')
                    .append(CsvField.text(t.title())).append(';')
                    .append(CsvField.capitalized(t.format().replace("_", " "))).append(';')
                    .append(t.durationMinutes()).append(';')
                    .append(CsvField.capitalized(t.level())).append(';')
                    .append(CsvField.text(speaker.name())).append(';')
                    .append(t.status()).append(';')
                    .append(DisplayFormats.norwegianDate(t.submittedAt())).append(';')
                    .append(t.averageRating() == null ? "" : String.valueOf(t.averageRating()).replace('.', ',')).append(';')
                    .append(t.ratingCount())
                    .append('\n');
        }
        return sb.toString();
    }
}
