## Quotes a value with a semicolon or a line break

The export separates the columns with ; and the talks with a line break. In a cell, \n is a
line feed and \r is a carriage return.


| Result | Scenario | Value | Exported? |
| --- | --- | --- | --- |
| ✅ passes | Plain text | Legacy is a feeling | Legacy is a feeling |
| ✅ passes | Semicolon | Legacy; a feeling | "Legacy; a feeling" |
| ✅ passes | Line feed | Legacy\nis a feeling | "Legacy\nis a feeling" |
| ✅ passes | Carriage return | Legacy\r\nis a feeling | "Legacy\r\nis a feeling" |
