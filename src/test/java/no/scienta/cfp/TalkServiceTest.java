package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TalkServiceTest {

    private SpeakerDao speakerDao;
    private TalkDao talkDao;
    private TalkService service;

    @BeforeEach
    void setUp() {
        speakerDao = mock(SpeakerDao.class);
        talkDao = mock(TalkDao.class);
        service = new TalkService(speakerDao, talkDao, mock(RatingService.class), mock(NotificationService.class),
                mock(Timestamps.class));
    }

    @Test
    void talkWithoutTitleIsRejected() {
        TalkForm form = form();
        form.title = " ";
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.submitTalk(form));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void unknownFormatIsRejected() {
        TalkForm form = form();
        form.format = "KEYNOTE";
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.submitTalk(form));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void unknownSpeakerIsRejected() {
        when(speakerDao.exists(7L)).thenReturn(false);
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.submitTalk(form()));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    private TalkForm form() {
        TalkForm form = new TalkForm();
        form.title = "Legacy is a feeling";
        form.format = "PRESENTATION";
        form.level = "INTERMEDIATE";
        form.speakerId = 7L;
        return form;
    }
}
