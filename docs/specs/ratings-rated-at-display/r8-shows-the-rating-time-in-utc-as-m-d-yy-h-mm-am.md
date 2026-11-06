## R8: Shows the rating time in UTC as M/d/yy, h:mm AM

The expected values have exactly one ordinary space before AM or PM, as the reviewer UI
needs. The service takes the time of the latest rating from its clock, which a test cannot
set, so this table checks only the format, through the formatter that PUT
/talks/{id}/ratings/{reviewerId} uses for ratedAtDisplay.


| Result | Scenario | Rated At | Shown? |
| --- | --- | --- | --- |
| ✅ passes | Afternoon | 2026-11-04T13:05:00Z | 11/4/26, 1:05 PM |
| ✅ passes | Midnight | 2026-11-04T00:00:00Z | 11/4/26, 12:00 AM |
| ✅ passes | Noon | 2026-11-04T12:00:00Z | 11/4/26, 12:00 PM |
| ✅ passes | Two-digit month, day and hour | 2026-12-24T10:30:00Z | 12/24/26, 10:30 AM |
| ✅ passes | Seconds in the time | 2026-11-04T13:05:59Z | 11/4/26, 1:05 PM |
| ✅ passes | Time given in another time zone | 2026-11-04T23:30:00-02:00 | 11/5/26, 1:30 AM |
