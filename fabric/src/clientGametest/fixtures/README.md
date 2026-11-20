# Client test fixtures

Owner-supplied inputs the client tests read from disk. The build passes this folder's path to the client-test runs as `somegoogly.clientgametest.fixtures`.

## `world-1.21.4/`

A singleplayer world saved by the 1.21.4 release of Some Googly Eyes, used by test 10.11. Copy the whole save folder (the one holding `level.dat`) here under this name. Before saving it, set it up so the first view on opening shows what the test expects:

- the player standing where an eyed mob with a tinted iris is in view;
- tinted Googly Eyes and a Slimy Eye in the hotbar;
- the player wearing no head, with their own eyes applied (seen in the F5 front shot);
- non-default values in `serverconfig/somegoogly-server.toml`, such as a changed `growOnHitPercent`.

Without this folder, test 10.11 is recorded in the review run as unfinished.
