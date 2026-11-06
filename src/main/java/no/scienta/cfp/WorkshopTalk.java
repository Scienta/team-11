package no.scienta.cfp;

import java.util.Optional;

/**
 * A talk as a registration sees it: its format and status. Decides whether the talk takes one more registration
 * (workshop-registration R2, R3, R5, R6), as a pure function of what the database holds.
 */
public record WorkshopTalk(String format, TalkStatus status) {

    /** Every workshop has room for this many attendees (R5). */
    public static final int SEATS = 20;

    /** Only a talk in the format {@code WORKSHOP} is a workshop (R2). */
    public boolean isWorkshop() {
        return "WORKSHOP".equals(format);
    }

    /** Why the talk refuses one more registration, or empty when it takes it. */
    public Optional<RegistrationOutcome> refusal(int seatsTaken, boolean emailRegistered) {
        if (!isWorkshop()) {
            return Optional.of(new RegistrationOutcome.NotAWorkshop());
        }
        if (status != TalkStatus.ACCEPTED) {
            return Optional.of(new RegistrationOutcome.Closed(status));
        }
        if (seatsTaken >= SEATS) {
            return Optional.of(new RegistrationOutcome.Full(SEATS));
        }
        if (emailRegistered) {
            return Optional.of(new RegistrationOutcome.AlreadyRegistered());
        }
        return Optional.empty();
    }
}
