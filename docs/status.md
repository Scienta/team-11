# Teststatus

Laget 6. november 2026 kl. 14:06 fra versjon [d66b3df](https://github.com/Scienta/team-11/commit/d66b3df99dab2e1d9efb1b8a488da2c9441f85bc).

✅ Våre tester: alle 50 regler er i orden (187 av 187 eksempler).

✅ Arrangørenes kontroller: alle 15 er i orden (Foredragsholdere, Foredrag, Karakterer).

Hver linje er en regel vi tester. Et eksempel er ett konkret tilfelle av regelen.
Arrangørenes kontroller sjekker tjenesten som kjører nå, og bare funksjoner vi har meldt ferdige.
Alle detaljer står i [testrapporten](specs/index.md).

## Foredragsholdere

- ✅ **R3** En foredragsholder må ha et navn. — 4 av 4 eksempler · [se eksemplene](specs/speakers/r3-registers-a-speaker-only-with-a-name.md)
- ✅ **R3** En foredragsholder må ha en gyldig e-postadresse. — 13 av 13 eksempler · [se eksemplene](specs/speakers/r3-registers-a-speaker-only-with-a-real-email-address.md)
- ✅ **R4** Beskrivelsen av en foredragsholder kan ha høyst 500 tegn. — 5 av 5 eksempler · [se eksemplene](specs/speakers/r4-limits-the-bio-to-500-characters.md)
- ✅ **R4** Grensen på 500 tegn gjelder også når en administrator endrer beskrivelsen. — 2 av 2 eksempler · [se eksemplene](specs/speakers/r4-applies-the-bio-limit-when-an-admin-updates-a-speaker.md)
- ✅ **R5** To foredragsholdere kan ikke ha samme e-postadresse. — 2 av 2 eksempler · [se eksemplene](specs/speakers/r5-refuses-a-second-speaker-with-the-same-email.md)
- ✅ **R7** En nettadresse med skråstrek til slutt virker som uten. — 2 av 2 eksempler · [se eksemplene](specs/speakers/r7-treats-a-trailing-slash-like-the-path-without-it.md)
- ✅ Vi finner en registrert foredragsholder igjen. — 3 av 3 eksempler · [se eksemplene](specs/speakers/finds-a-registered-speaker-by-id.md)

## Foredrag

- ✅ **R3** Et foredrag må ha en tittel. — 4 av 4 eksempler · [se eksemplene](specs/talks/r3-accepts-a-talk-only-with-a-real-title.md)
- ✅ **R4** Lengden på foredraget følger av formatet. Foredragsholderen velger den ikke. — 4 av 4 eksempler · [se eksemplene](specs/talks/r4-sets-the-duration-from-the-format.md)
- ✅ **R5** Et foredrag har ett av de fire formatene: lyntale på 10 eller 20 minutter, presentasjon eller workshop. — 4 av 4 eksempler · [se eksemplene](specs/talks/r5-accepts-only-the-four-formats-in-capitals.md)
- ✅ **R5** Et foredrag har ett av de tre nivåene: nybegynner, middels eller viderekommen. — 4 av 4 eksempler · [se eksemplene](specs/talks/r5-accepts-only-the-three-levels-in-capitals.md)

## Karakterer

- ✅ **R1** Hver vurderer har et nummer. Vi tar bare imot gyldige numre. — 6 av 6 eksempler · [se eksemplene](specs/ratings/r1-accepts-a-reviewer-from-1-to-9223372036854775807.md)
- ✅ **R2** Karakteren er et helt tall fra 1 til 5. — 7 av 7 eksempler · [se eksemplene](specs/ratings/r2-accepts-only-a-whole-number-from-1-to-5-as-score.md)
- ✅ **R3** Hver vurderer har én karakter per foredrag. Den siste karakteren gjelder. — 2 av 2 eksempler · [se eksemplene](specs/ratings/r3-keeps-one-rating-per-reviewer-the-latest-score.md)
- ✅ **R4** Snittet vises med én desimal. En halv rundes opp. — 3 av 3 eksempler · [se eksemplene](specs/ratings/r4-shows-the-average-score-with-one-decimal-halves-up.md)
- ✅ **R5** Listen viser alle foredrag med det eldste først, eller bare foredragene til én foredragsholder. — 4 av 4 eksempler · [se eksemplene](specs/ratings-talk-lists/r5-lists-all-talks-oldest-first-or-the-talks-of-one-speaker.md)
- ✅ **R6** Rangeringen går etter snittet, så etter antall karakterer, så etter hvor gammelt foredraget er. — 5 av 5 eksempler · [se eksemplene](specs/ratings-talk-lists/r6-ranks-talks-by-shown-average-then-rating-count-then-age.md)
- ✅ Listen er bare rangert når man ber om det. Ellers kommer det eldste foredraget først. — 4 av 4 eksempler · [se eksemplene](specs/ratings-talk-lists/ranks-talks-only-for-the-sort-order-rating.md)
- ✅ **R7** Karakterer som kommer samtidig, telles riktig: hver vurderer én gang. — 3 av 3 eksempler · [se eksemplene](specs/ratings-ratings-at-the-same-time/r7-counts-each-reviewer-once-when-ratings-arrive-together.md)
- ✅ **R8** Tidspunktet for siste karakter vises i verdenstid, slik vurderingsverktøyet trenger det (for eksempel 11/6/26, 2:05 PM). — 6 av 6 eksempler · [se eksemplene](specs/ratings-rated-at-display/r8-shows-the-rating-time-in-utc-as-m-d-yy-h-mm-am.md)

## Godkjenning

- ✅ **R1** Komiteen kan godta, avslå eller sette et foredrag på venteliste. Andre valg avvises. — 4 av 4 eksempler · [se eksemplene](specs/acceptance/r1-takes-only-the-three-decisions-as-a-status.md)
- ✅ **R2** Komiteens beslutning gir foredraget ny status. — 7 av 7 eksempler · [se eksemplene](specs/acceptance/r2-moves-a-talk-to-the-committee-s-decision.md)
- ✅ **R3** Et foredrag kan bare godtas når to forskjellige vurderere har gitt karakter. — 6 av 6 eksempler · [se eksemplene](specs/acceptance/r3-accepts-a-talk-only-after-two-different-reviewers.md)
- ✅ **R4** Bare hovedforedragsholderen kan trekke et foredrag. — 3 av 3 eksempler · [se eksemplene](specs/acceptance/r4-lets-only-the-main-speaker-withdraw-a-talk.md)
- ✅ **R4** Et foredrag kan trekkes, unntatt når det er avslått eller allerede trukket. — 3 av 3 eksempler · [se eksemplene](specs/acceptance/r4-withdraws-a-talk-unless-it-is-rejected-or-withdrawn.md)
- ✅ **R5** Foredraget viser samme status overalt der det vises. — 5 av 5 eksempler · [se eksemplene](specs/acceptance/r5-shows-the-current-status-wherever-the-talk-is-shown.md)
- ✅ **R7** Et trukket foredrag teller ikke med i grensen for antall foredrag. — 2 av 2 eksempler · [se eksemplene](specs/acceptance/r7-leaves-a-withdrawn-talk-out-of-the-talk-limit.md)
- ✅ **R8** Et foredrag får bare nye karakterer så lenge komiteen ikke har bestemt seg. — 2 av 2 eksempler · [se eksemplene](specs/acceptance/r8-takes-ratings-only-for-a-talk-that-is-still-open.md)
- ✅ **R9** Trukne foredrag kommer sist i listene. — 7 av 7 eksempler · [se eksemplene](specs/acceptance/r9-shows-withdrawn-talks-after-the-others.md)
- ✅ **R9** Man kan velge å skjule trukne foredrag. Valget er ja eller nei. — 3 av 3 eksempler · [se eksemplene](specs/acceptance/r9-takes-only-true-or-false-to-leave-out-withdrawn-talks.md)
- ✅ Vi sier tydelig fra når et foredrag ikke finnes. — 1 av 1 eksempel · [se eksemplene](specs/acceptance/answers-404-for-a-talk-that-does-not-exist.md)

## Workshoper

- ✅ **R1** En påmelding må ha et navn og en gyldig e-postadresse. — 9 av 9 eksempler · [se eksemplene](specs/workshop-registration/r1-registers-an-attendee-only-with-a-name-and-a-real-email.md)
- ✅ En påmelding som ikke kan leses, avvises med en tydelig feil. — 5 av 5 eksempler · [se eksemplene](specs/workshop-registration/answers-400-for-a-request-it-cannot-use.md)
- ✅ **R2** Bare workshoper tar imot påmeldinger. — 3 av 3 eksempler · [se eksemplene](specs/workshop-registration/r2-takes-registrations-only-for-a-workshop.md)
- ✅ **R3** Man kan bare melde seg på en workshop som komiteen har godtatt. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r3-takes-registrations-only-for-an-accepted-workshop.md)
- ✅ **R4** Når en workshop trekkes, står de påmeldte fortsatt på listen. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r4-keeps-the-attendees-of-a-withdrawn-workshop.md)
- ✅ **R5** En workshop har plass til høyst 20 deltakere. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r5-takes-at-most-20-attendees-for-a-workshop.md)
- ✅ **R5** Også når mange melder seg på samtidig, får bare 20 plass. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r5-keeps-20-seats-when-many-register-at-the-same-time.md)
- ✅ **R6, R7** En e-postadresse kan bare meldes på én gang per workshop, uansett store eller små bokstaver. Samme person kan melde seg på flere workshoper. — 3 av 3 eksempler · [se eksemplene](specs/workshop-registration/r6-r7-registers-an-email-once-for-each-workshop.md)
- ✅ **R6** Kommer samme påmelding flere ganger samtidig, teller den bare én gang. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r6-registers-one-of-the-same-email-sent-at-the-same-time.md)
- ✅ **R8** Arrangørene ser deltakerne i den rekkefølgen de meldte seg på. — 3 av 3 eksempler · [se eksemplene](specs/workshop-registration/r8-lists-the-attendees-in-the-order-they-registered.md)
- ✅ **R9** En deltaker kan melde seg av, og plassen blir ledig for en annen. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r9-frees-the-seat-when-an-attendee-unregisters.md)
- ✅ **R9** En påmelding kan bare meldes av én gang, og bare fra sin egen workshop. — 5 av 5 eksempler · [se eksemplene](specs/workshop-registration/r9-unregisters-a-registration-once-from-its-own-workshop.md)
- ✅ **R6, R9** Den som har meldt seg av, kan melde seg på igjen. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r6-r9-registers-an-email-again-after-it-unregistered.md)
- ✅ **R9** En ledig plass går til én deltaker, også når mange melder seg på samtidig. — 2 av 2 eksempler · [se eksemplene](specs/workshop-registration/r9-gives-a-freed-seat-to-one-attendee-only.md)

## Oppstart

- ✅ **R1** Ved oppstart varmer tjenesten seg opp med de samme forespørslene som brukerne sender. — 6 av 6 eksempler · [se eksemplene](specs/warm-up/r1-sends-each-warm-up-request-through-the-http-port.md)
- ✅ **R2** Oppvarmingen etterlater ingen data. — 1 av 1 eksempel · [se eksemplene](specs/warm-up/r2-leaves-every-table-as-empty-as-it-found-it.md)
- ✅ **R3** Tjenesten skriver i loggen hvor mange forespørsler oppvarmingen sendte. — 1 av 1 eksempel · [se eksemplene](specs/warm-up/r3-writes-the-number-of-requests-when-the-warm-up-ends.md)
- ✅ **R4** En forespørsel som ikke får svar, regnes som en feil. — 2 av 2 eksempler · [se eksemplene](specs/warm-up-http-exchange/r4-turns-a-request-without-an-answer-into-a-failure.md)
- ✅ **R4** Tjenesten varsler i loggen om et uventet svar eller en feil under oppvarmingen. — 3 av 3 eksempler · [se eksemplene](specs/warm-up-outcomes/r4-warns-about-an-unexpected-status-or-a-failure.md)
