## R6: Quotes a field only when it has to

Each row is talk 7, a PRESENTATION in R1 S1. A title that starts like a spreadsheet formula
is written as it is: the request asks for every other field as it is.


| Result | Scenario | Title | Main Speaker | CSV Line? |
| --- | --- | --- | --- | --- |
| ✅ passes | Plain title | Legacy is a feeling | Grace Hopper | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,Grace Hopper |
| ✅ passes | Comma in the title | Kotlin, really fast | Grace Hopper | S1,09:00,10:00,R1,7,"Kotlin, really fast",PRESENTATION,Grace Hopper |
| ✅ passes | Comma and quotes | Kotlin, "really" fast | Grace Hopper | S1,09:00,10:00,R1,7,"Kotlin, ""really"" fast",PRESENTATION,Grace Hopper |
| ✅ passes | Quotes only | Say "hi" | Grace Hopper | S1,09:00,10:00,R1,7,"Say ""hi""",PRESENTATION,Grace Hopper |
| ✅ passes | Newline in the title | Two⏎lines | Grace Hopper | S1,09:00,10:00,R1,7,"Two⏎lines",PRESENTATION,Grace Hopper |
| ✅ passes | Carriage return | Two␍⏎lines | Grace Hopper | S1,09:00,10:00,R1,7,"Two␍⏎lines",PRESENTATION,Grace Hopper |
| ✅ passes | Starts like a formula | =SUM(1;2) | Grace Hopper | S1,09:00,10:00,R1,7,=SUM(1;2),PRESENTATION,Grace Hopper |
| ✅ passes | Spaces around the title | &#x2423;&#x2423;Spaces around&#x2423;&#x2423; | Grace Hopper | S1,09:00,10:00,R1,7,&#x2423;&#x2423;Spaces around&#x2423;&#x2423;,PRESENTATION,Grace Hopper |
| ✅ passes | Comma in a speaker name | Legacy is a feeling | Hopper, Grace | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,"Hopper, Grace" |
| ✅ passes | Quotes in a speaker name | Legacy is a feeling | Grace "Amazing" Hopper | S1,09:00,10:00,R1,7,Legacy is a feeling,PRESENTATION,"Grace ""Amazing"" Hopper" |
