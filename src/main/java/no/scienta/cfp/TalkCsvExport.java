package no.scienta.cfp;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * All talks as semicolon-separated values for the programme committee's spreadsheet.
 */
@Component
public class TalkCsvExport {

    private final TalkService talkService;
    private final SpeakerService speakers;

    public TalkCsvExport(TalkService talkService, SpeakerService speakers) {
        this.talkService = talkService;
        this.speakers = speakers;
    }

    public String csv() {
        List<TalkDto> talks = talkService.listTalks(null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("id;title;format;minutes;level;speaker;status;submitted;rating;ratings\n");
        for (TalkDto t : talks) {
            Speaker speaker = speakers.getSpeaker(t.speakerId);
            sb.append(t.id).append(';')
                    .append(Utils.csv(t.title)).append(';')
                    .append(Utils.capitalize(t.format.replace("_", " "))).append(';')
                    .append(t.durationMinutes).append(';')
                    .append(Utils.capitalize(t.level)).append(';')
                    .append(Utils.csv(speaker.name())).append(';')
                    .append(t.status).append(';')
                    .append(Utils.norwegianDate(t.submittedAt)).append(';')
                    .append(t.averageRating == null ? "" : String.valueOf(t.averageRating).replace('.', ',')).append(';')
                    .append(t.ratingCount)
                    .append('\n');
        }
        return sb.toString();
    }
}
