# T-028 TV guide: layout errors and look

## Goal

Maintainer request (22 Nov 2026): the guide is good but has layout errors and could look better. Approved as T-028.

## Findings (`PlayerGuide.kt`)

- Time marks at the left and right edge were cut in half (labels drawn from x > -100 dp in a clipped box).
- The detail panel has a fixed height (header row 238 dp); the description used `maxLines = 6`, so long texts were cut through the middle of a line.
- Channel cells: empty box without a logo, no number, provider prefix ("CA: ") eats the narrow name.
- Shows of one or two minutes became 3 dp slivers with a border and 12 dp corners.
- `nowSec` (30 s tick) went into every row: all visible rows and shows recomposed every 30 s; one "now" line box per row.
- Every focus step in the categories switched the grid (rows rebuilt, program preloading started) – the same lag as in the channel menu.
- Day marks used the German pattern `EE dd.MM.` in every language (`Epg.kt` `dayLabel`).
- Focused show: 62 % accent under white text, hard to read with light accents (IPVIO / DubTvP green).

## Change

- `GuideGridLogic.kt` (new, pure): day pattern per language, header mark fit, block width and corner radius, filter for fillers under 2 minutes, lines that fit.
- `Epg.kt`: `dayLabel` uses the pattern of the UI language (de unchanged).
- `PlayerGuide.kt`:
  - Header marks only when they fit, with small ticks; full hours brighter.
  - Detail panel: as many whole description lines as fit, the last one ends with "…"; channel logo next to the title.
  - Channel column 224 dp: number (same as Live TV), logo or monogram, name without the provider prefix (the full name stays in the detail panel).
  - Fillers under 2 minutes are not drawn; narrow blocks have small corners and no border.
  - "Now" as a state object: shows recompose only when they change from later to now to past; progress bars and one "now" line over the whole grid are drawn in the draw phase.
  - Categories: the grid follows the focus after 320 ms of rest and never while Up/Down is held; OK or Right switches at once and moves the focus into the grid. Held keys throttled, list jumps instead of animating.
  - Look: theme background, category rows like the channel menu (glass, frame in the highlight color, bar on the chosen one), calmer focus gradient on shows, slightly bluer opaque block colors.

## Not changed

EPG loading and caching (`Epg`, `EpgFull`, `Xmltv`), reminders, archive playback, collections, guide settings and their keys.

## Acceptance criteria

1. No half time marks at the edges, also after moving right and left.
2. Long descriptions end with "…" on a whole line.
3. Every channel cell shows a number and a logo or letters.
4. No slivers; short shows are still reachable if they are 2 minutes or longer.
5. Stepping through the categories with a held key is smooth; the grid changes when the focus rests or on OK/Right.
6. Day marks in French read like "dim. 22 nov.".

## Risks

- Shows under 2 minutes can no longer be focused (they could not be read before either).
- Prefix stripping: in "Alle Sender" with several countries the country prefix is no longer visible in the channel cell (the detail panel shows the full name).
- Not compiled; the `BoxWithConstraints` in the detail panel and the draw-phase reads were checked by reading only.

## Test results

- Ran on the JVM: `GuideGridLogicTest` 6 of 6 (the French day mark checked with the desktop JDK locale data; Android uses ICU, the text can differ slightly).
- Syntax check (parse only) of `PlayerGuide.kt` and `Epg.kt`: no syntax errors. Not compiled against Android/Compose.
- Not run: Gradle build, device test.

## Test steps for the maintainer

1. Open the guide in the player. Move right several hours and back: the time marks are complete at both edges; the "now" line runs through all rows.
2. Pick a show with a long description: the text ends with "…" on a whole line, nothing is cut.
3. Channel cells: numbers match Live TV; channels without a logo show letters.
4. Hold Down in the categories: smooth; stop: the grid follows. OK and Right switch at once and the focus lands in the grid.
5. Leave the guide open for over a minute on a running show: the progress bar moves, the show turns "past" at its end.
6. Language French and English: day marks at midnight read naturally.

## Rollback

Revert the commit (`PlayerGuide.kt`, `GuideGridLogic.kt`, `Epg.kt`).

## Status

Implemented, not committed by the maintainer yet, not built, not tested on a device.
