## R3: Rounds the exact average to 2 decimals, ties to even

Each score comes from a different reviewer; 4x39 means 39 reviewers who each gave 4.
The rows with 42 ratings keep 40 scores; their mean, as a double, lies just off the tie and
rounds the wrong way. Only the exact mean rounds as the table says.


| Result | Scenario | Scores | Average Rating? | Rating Count? |
| --- | --- | --- | --- | --- |
| ✅ passes | 3.125 goes down to the even 3.12 | [5, 4, 3x7, 1] | 3.12 | 10 |
| ✅ passes | 3.375 goes up to the even 3.38 | [5, 4x3, 3x5, 1] | 3.38 | 10 |
| ✅ passes | 4.025 goes to 4.02, a double to 4.03 | [5x2, 4x39, 1] | 4.02 | 42 |
| ✅ passes | 4.475 goes to 4.48, a double to 4.47 | [5x20, 4x21, 1] | 4.48 | 42 |
| ✅ passes | 2.175 goes to 2.18, a double to 2.17 | [5, 3x7, 2x33, 1] | 2.18 | 42 |
| ✅ passes | 1.333... is not a tie | [1, 1, 2] | 1.33 | 3 |
