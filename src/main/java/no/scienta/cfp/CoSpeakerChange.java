package no.scienta.cfp;

/**
 * What a request to add or remove a co-speaker did to a talk: it changed, or did not need to, or one of the reasons
 * why it was refused (Co-speakers R1–R3, R5, R8, R9, R12).
 */
public sealed interface CoSpeakerChange {

    /** The talk's co-speakers are as asked (R1, R5, R8). The talk is as the API shows it, ratings included. */
    record Changed(TalkDto talk) implements CoSpeakerChange {
    }

    /** Every outcome in which the co-speakers did not change. */
    sealed interface Refusal extends CoSpeakerChange {
    }

    /** No talk has this id. */
    record TalkNotFound() implements Refusal {
    }

    /** The request says who acts, and it is not the talk's main speaker (R12). */
    record NotMainSpeaker() implements Refusal {
    }

    /** No registered speaker has this id (R1). */
    record SpeakerNotFound() implements Refusal {
    }

    /** The talk is withdrawn or rejected, so its co-speakers can't change (R9). */
    record Closed() implements Refusal {
    }

    /** The speaker is the talk's main speaker, who can't be added or removed as a co-speaker (R3, R8). */
    record MainSpeaker() implements Refusal {
    }

    /** The talk already has the most co-speakers it can have (R2). */
    record Full(int coSpeakers) implements Refusal {
    }
}
