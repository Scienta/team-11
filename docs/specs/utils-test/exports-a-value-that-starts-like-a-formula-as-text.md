## Exports a value that starts like a formula as text

A spreadsheet runs a cell that starts with =, +, - or @ as a formula, also after a tab. The
export of /admin/talks.csv puts ' in front, as the OWASP advice on CSV injection says. A
title such as "-1 tips" also gets the ', because the export cannot tell it from a formula.
In a cell, \t is a tab.


| Result | Scenario | Value | Exported? |
| --- | --- | --- | --- |
| ✅ passes | Starts with = | =1+1 | '=1+1 |
| ✅ passes | Starts with + | +47 22 33 44 55 | '+47 22 33 44 55 |
| ✅ passes | Starts with - | -1 tips | '-1 tips |
| ✅ passes | Starts with @ | @SUM(A1) | '@SUM(A1) |
| ✅ passes | Starts with a tab | \t=1+1 | '\t=1+1 |
| ✅ passes | Formula sign after the start | 1+1=2 | 1+1=2 |
