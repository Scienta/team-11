# Teststatus

Laget 6. november 2026 kl. 15:18 fra versjon [2355a81](https://github.com/Scienta/team-11/commit/2355a811272b3e9dab7280fe69186c6ee430c0c6).

✅ Våre tester: alle 83 regler er i orden (364 av 364 eksempler).

❌ Arrangørenes kontroller: 2 av 33 er ikke i orden (Medforedragsholdere).

Hver linje er en regel vi tester. Et eksempel er ett konkret tilfelle av regelen.
Arrangørenes kontroller sjekker tjenesten som kjører nå, og bare funksjoner vi har meldt ferdige.
Alle detaljer står i [testrapporten](specs/index.md).

## Foredragsholdere

- ✅ **R3** En foredragsholder må ha et navn. — 4 av 4 eksempler · [se eksemplene](specs/speakers/r3-registers-a-speaker-only-with-a-name.md)
- ✅ **R3** En foredragsholder må ha en gyldig e-postadresse. — 17 av 17 eksempler · [se eksemplene](specs/speakers/r3-registers-a-speaker-only-with-a-real-email-address.md)
- ✅ **R4** Beskrivelsen av en foredragsholder kan ha høyst 500 tegn. — 5 av 5 eksempler · [se eksemplene](specs/speakers/r4-limits-the-bio-to-500-characters.md)
- ✅ Ingen kan endre en foredragsholder etter registreringen. — 2 av 2 eksempler · [se eksemplene](specs/speakers/refuses-to-change-a-registered-speaker.md)
- ✅ **R5** To foredragsholdere kan ikke ha samme e-postadresse. — 2 av 2 eksempler · [se eksemplene](specs/speakers/r5-refuses-a-second-speaker-with-the-same-email.md)
- ✅ **R7** En nettadresse med skråstrek til slutt virker som uten. — 2 av 2 eksempler · [se eksemplene](specs/speakers/r7-treats-a-trailing-slash-like-the-path-without-it.md)
- ✅ Vi finner en registrert foredragsholder igjen. — 3 av 3 eksempler · [se eksemplene](specs/speakers/finds-a-registered-speaker-by-id.md)

## Foredrag

- ✅ **R3** Et foredrag må ha en tittel. — 8 av 8 eksempler · [se eksemplene](specs/talks/r3-accepts-a-talk-only-with-a-real-title.md)
- ✅ **R4** Lengden på foredraget følger av formatet. Foredragsholderen velger den ikke. — 4 av 4 eksempler · [se eksemplene](specs/talks/r4-sets-the-duration-from-the-format.md)
- ✅ **R5** Et foredrag har ett av de fire formatene: lyntale på 10 eller 20 minutter, presentasjon eller workshop. — 4 av 4 eksempler · [se eksemplene](specs/talks/r5-accepts-only-the-four-formats-in-capitals.md)
- ✅ **R5** Et foredrag har ett av de tre nivåene: nybegynner, middels eller viderekommen. — 4 av 4 eksempler · [se eksemplene](specs/talks/r5-accepts-only-the-three-levels-in-capitals.md)
- ✅ **R3, R5** Et foredrag som mangler noe, blir avvist, og svaret sier hva som mangler. — 6 av 6 eksempler · [se eksemplene](specs/talks/r3-r5-refuses-an-incomplete-talk-with-the-reason.md)
- ✅ **R2, R8** Spør du etter et foredrag eller en foredragsholder som ikke finnes, får du svar om at det ikke finnes. — 2 av 2 eksempler · [se eksemplene](specs/talks/r2-r8-answers-404-for-a-talk-or-speaker-that-does-not-exist.md)
- ✅ **R1, R2, R3, R5** Et foredrag kan ha høyst to medforedragsholdere. De må være registrert, og hovedforedragsholderen kan ikke være en av dem. — 10 av 10 eksempler · [se eksemplene](specs/co-speakers/r1-r2-r3-r5-adds-another-registered-speaker-at-most-two.md)
- ✅ **R4** Alle foredrag viser medforedragsholderne i den rekkefølgen de ble lagt til. — 7 av 7 eksempler · [se eksemplene](specs/co-speakers/r4-shows-the-co-speakers-last-in-the-order-added-wherever-a-talk-is-shown.md)
- ✅ **R4** Medforedragsholderne vises også når et foredrag blir godkjent, avvist, satt på venteliste eller trukket. — 2 av 2 eksempler · [se eksemplene](specs/co-speakers/r4-shows-the-co-speakers-in-the-answers-of-the-acceptance-endpoints.md)
- ✅ **R6** En foredragsholders egen liste viser bare foredrag der hen er hovedforedragsholder. — 2 av 2 eksempler · [se eksemplene](specs/co-speakers/r6-a-co-speaker-s-own-talk-list-shows-only-their-own-talks.md)
- ✅ **R8** En medforedragsholder kan fjernes igjen. Hovedforedragsholderen kan ikke fjernes. — 7 av 7 eksempler · [se eksemplene](specs/co-speakers/r8-removes-a-co-speaker-never-the-main-speaker.md)
- ✅ **R9** Et foredrag som er trukket eller avvist, kan ikke få nye eller færre medforedragsholdere. — 2 av 2 eksempler · [se eksemplene](specs/co-speakers/r9-a-withdrawn-or-rejected-talk-can-t-be-changed.md)
- ✅ Et ukjent foredrag gir en tydelig feilmelding. — 4 av 4 eksempler · [se eksemplene](specs/co-speakers/answers-404-for-an-unknown-talk-and-400-for-a-talk-id-that-is-not-a-number.md)
- ✅ **R12** Når forespørselen sier hvem som gjør endringen, kan bare hovedforedragsholderen legge til eller fjerne medforedragsholdere. — 12 av 12 eksempler · [se eksemplene](specs/co-speakers/r12-only-the-main-speaker-changes-the-co-speakers-when-the-request-says-who-acts.md)
- ✅ **R12** Et ukjent foredrag gir fortsatt 404. Hvem som gjør endringen, sjekkes før foredragets status. — 5 av 5 eksempler · [se eksemplene](specs/co-speakers/r12-who-acts-is-checked-after-the-talk-is-found-and-before-the-other-rules.md)

## Karakterer

- ✅ **R1** Hver vurderer har et nummer. Vi tar bare imot gyldige numre. — 6 av 6 eksempler · [se eksemplene](specs/ratings/r1-accepts-a-reviewer-from-1-to-9223372036854775807.md)
- ✅ **R1, R2** En ugyldig karakter blir avvist, og svaret sier hvorfor. — 6 av 6 eksempler · [se eksemplene](specs/ratings/r1-r2-refuses-a-rating-with-the-reason-before-it-looks-for-the-talk.md)
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
- ✅ **R10** Den som melder seg på, får en hemmelig avmeldingskode. Bare med den koden kan påmeldingen meldes av. — 8 av 8 eksempler · [se eksemplene](specs/workshop-registration/r10-unregisters-only-with-the-registration-s-cancellation-token.md)

## Program

- ✅ **R1** Programmet har tre rom med de samme fem tidspunktene i hvert rom. Start og slutt er faste. — 5 av 5 eksempler · [se eksemplene](specs/programme-scheduling/r1-gives-each-slot-its-fixed-start-and-end.md)
- ✅ **R1** Bare rommene og tidspunktene som finnes, kan brukes. Andre avvises. — 4 av 4 eksempler · [se eksemplene](specs/programme-scheduling/r1-refuses-a-room-or-slot-outside-the-venue.md)
- ✅ **R2** Komiteen kan sette et foredrag i et rom på et tidspunkt, se hva som står der, og ta det ut igjen. — 8 av 8 eksempler · [se eksemplene](specs/programme-scheduling/r2-puts-a-talk-in-a-place-looks-it-up-takes-it-out.md)
- ✅ **R2** For å sette et foredrag på programmet må komiteen oppgi et foredrag som finnes. — 11 av 11 eksempler · [se eksemplene](specs/programme-scheduling/r2-puts-a-talk-only-for-a-body-with-a-talk-id.md)
- ✅ **R3** Bare godtatte foredrag kommer på programmet. — 2 av 2 eksempler · [se eksemplene](specs/programme-scheduling/r3-puts-only-an-accepted-talk-on-the-programme.md)
- ✅ **R4** En workshop må ligge på det lange tidspunktet, fra 13:30 til 15:30. Andre foredrag passer på alle tidspunkter. — 3 av 3 eksempler · [se eksemplene](specs/programme-scheduling/r4-puts-a-workshop-only-in-the-long-slot.md)
- ✅ **R5** Det er ett foredrag per rom og tidspunkt, og et foredrag står bare én gang på programmet. For å flytte et foredrag tar man det ut først. — 4 av 4 eksempler · [se eksemplene](specs/programme-scheduling/r5-keeps-one-talk-per-place-and-each-talk-once.md)
- ✅ **R6** Ingen foredragsholder eller medforedragsholder skal være i to rom på samme tidspunkt. — 6 av 6 eksempler · [se eksemplene](specs/programme-scheduling/r6-keeps-each-speaker-in-one-room-at-a-time.md)
- ✅ **R7** Når et foredrag endres etterpå, for eksempel trekkes, står programmet som før. Komiteen kan ta det ut. — 3 av 3 eksempler · [se eksemplene](specs/programme-scheduling/r7-leaves-the-programme-as-it-is-when-a-talk-changes.md)
- ✅ **R8** Også når flere i komiteen endrer programmet samtidig, holder reglene. — 3 av 3 eksempler · [se eksemplene](specs/programme-scheduling/r8-keeps-the-rules-when-requests-come-at-the-same-time.md)

## Oppstart

- ✅ **R1** Ved oppstart varmer tjenesten seg opp med de samme forespørslene som brukerne sender. — 6 av 6 eksempler · [se eksemplene](specs/warm-up/r1-sends-each-warm-up-request-through-the-http-port.md)
- ✅ **R2** Oppvarmingen etterlater ingen data. — 1 av 1 eksempel · [se eksemplene](specs/warm-up/r2-leaves-every-table-as-empty-as-it-found-it.md)
- ✅ **R3** Tjenesten skriver i loggen hvor mange forespørsler oppvarmingen sendte. — 1 av 1 eksempel · [se eksemplene](specs/warm-up/r3-writes-the-number-of-requests-when-the-warm-up-ends.md)
- ✅ **R4** En forespørsel som ikke får svar, regnes som en feil. — 2 av 2 eksempler · [se eksemplene](specs/warm-up-http-exchange/r4-turns-a-request-without-an-answer-into-a-failure.md)
- ✅ **R4** Tjenesten varsler i loggen om et uventet svar eller en feil under oppvarmingen. — 3 av 3 eksempler · [se eksemplene](specs/warm-up-outcomes/r4-warns-about-an-unexpected-status-or-a-failure.md)

## Annet

- ✅ Ved en feil i databasen svarer tjenesten uten å vise tekniske detaljer. — 1 av 1 eksempel · [se eksemplene](specs/error-answers/answers-a-database-failure-without-its-details.md)
- ✅ Tekst som inneholder et ugyldig nulltegn, avvises. — 2 av 2 eksempler · [se eksemplene](specs/error-answers/refuses-a-text-field-that-holds-a-nul-character.md)
- ✅ E-postadresser skjules i loggen. — 7 av 7 eksempler · [se eksemplene](specs/request-log-filter-test/masks-each-email-in-the-logged-bodies.md)
- ✅ Avmeldingskoder skjules i loggen. — 10 av 10 eksempler · [se eksemplene](specs/request-log-filter-test/masks-each-cancellation-token-in-the-logged-bodies.md)
- ✅ Også en stor forespørsel blir logget raskt. — 4 av 4 eksempler · [se eksemplene](specs/request-log-filter-test/logs-a-large-body-in-a-short-time.md)
- ✅ I eksporten til regneark blir en verdi som ligner en formel, lagret som tekst. — 6 av 6 eksempler · [se eksemplene](specs/csv-field-test/exports-a-value-that-starts-like-a-formula-as-text.md)
- ✅ I eksporten til regneark får en verdi med semikolon eller linjeskift anførselstegn rundt seg. — 4 av 4 eksempler · [se eksemplene](specs/csv-field-test/quotes-a-value-with-a-semicolon-or-a-line-break.md)
- ✅ I statistikken vises snittet av foredragenes snitt med én desimal. En halv rundes opp. — 4 av 4 eksempler · [se eksemplene](specs/reports/averages-the-talks-shown-averages-one-decimal-halves-up.md)
- ✅ I statistikken vises samlet lengde på alle foredrag i hele timer. En halv time rundes opp. — 6 av 6 eksempler · [se eksemplene](specs/reports/counts-the-total-length-of-all-talks-in-whole-hours-rounded.md)
- ✅ Tjenesten avviser en forespørsel som er større enn 1 MB. — 3 av 3 eksempler · [se eksemplene](specs/request-limits/refuses-a-request-body-larger-than-1-mb.md)
