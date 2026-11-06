## Refuses to change a registered speaker

Speaker 1 registers as Grace Hopper. The request asks to change the name to Ada Lovelace.
No spec has an endpoint to change a speaker, and the old admin update had no
authorisation (security review, 2026-11-06).


| Result | Scenario | Path | Status? | Name After? |
| --- | --- | --- | --- | --- |
| ✅ passes | Without slash | /speakers/1 | 405 | Grace Hopper |
| ✅ passes | With slash | /speakers/1/ | 405 | Grace Hopper |
