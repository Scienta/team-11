package no.scienta.cfp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The guard behind the row lock in {@link WorkshopRegistrationService}: when the unique index finds the email after all,
 * the outcome is still a refusal, not an error (workshop-registration R6).
 */
class WorkshopRegistrationServiceTest {

    @DisplayName("R6: Refuses the registration when the save finds the email already on the list")
    @Test
    void refusesWhenTheSaveFindsTheEmailAlreadyOnTheList() {
        WorkshopRegistrationDao dao = mock(WorkshopRegistrationDao.class);
        RegistrationRequest ada = new RegistrationRequest("Ada Lovelace", "ada@example.com");
        when(dao.lockTalk(1L)).thenReturn(Optional.of(new WorkshopTalk("WORKSHOP", "ACCEPTED")));
        when(dao.seatsTaken(1L)).thenReturn(0);
        when(dao.emailRegistered(1L, "ada@example.com")).thenReturn(false);
        when(dao.insert(1L, ada)).thenReturn(Optional.empty());

        RegistrationOutcome outcome = new WorkshopRegistrationService(dao).register(1L, ada);

        assertEquals(new RegistrationOutcome.AlreadyRegistered(), outcome);
    }
}
