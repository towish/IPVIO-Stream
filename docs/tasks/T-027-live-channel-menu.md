# T-027 Live player: fluid channel menu (Left key)

## Goal

Maintainer request (22 Nov 2026): the side menu of Live TV in the player is not fluid and not functional. Approved as T-027.

## Findings (`PlayerLive.kt`, `ChannelOverlay`)

- Every focus step in the category column switched the channel list (`onFocus = { onChanCat(g) }`): new list, scroll, rows and program requests on each step. Main cause of the lag.
- The category column had no list state: when the selected category was off screen, its focus target was not composed and the focus was lost.
- Category order differed from the Live TV tab (no provider order, no `CatOrder`, hidden categories shown).
- Channel rows had a variable height (two lines or three with program) and loaded the program on the main thread; no logo meant an empty box; no channel number.
- The focused channel was state of `PlayerScreen`, so every step in the list recomposed the player content.
- `byId`, `groupBy` and the numbers were rebuilt on the main thread every time the menu opened.
- Hidden or vanished category as current selection, or an empty selection (no favorites): no focus target, keys went nowhere.

## Change

- `ChannelMenuLogic.kt` (new, pure): category order (favorite categories, provider order, Québec pinning when T-026 is on, `CatOrder`, hidden left out), first visible row, monogram for channels without a logo.
- `PlayerLive.kt`:
  - Category column: the list follows the focus only after 320 ms of rest and never while Up/Down is held; OK or Right opens the focused category at once. Opens scrolled to the selected category. Held keys are throttled as in Live TV (`liveHoldThrottle`, `holdJump`). A hidden current category puts the focus on "Alle Sender".
  - Lookups (by id, by category, numbers) cached per list and built in the background for lists over 4,000 channels.
  - The focused channel lives in the menu (`focus` state); only the info bar reads it. `onFocusedCh` is still called after 600 ms of rest.
  - Channel rows: fixed height (follows the "Senderzeilen" setting), number, logo or monogram (also when the logo fails to load), program on `Dispatchers.Default`, focus as light glass with a frame in the highlight color (content not redrawn on focus), current channel marked.
  - Empty selection: a focusable line "Keine Sender in dieser Auswahl".
  - One panel with the theme background; the info bar is hidden while the categories are open; the two hint texts moved to the bottom of their columns (existing dictionary rows).
- `DictQ.kt`: one new text.

## Not changed

`PlayerScreen.kt` (same `ChannelOverlay` parameters), zapping, favorites and channel lists, the channel menu (`ChannelMenu`), long-OK behaviour.

## Acceptance criteria

1. Holding Up/Down in the categories moves smoothly; the channel list changes only when the focus rests.
2. OK or Right on a category shows its channels with the focus in the list.
3. Opening the categories puts the focus on the selected category, also far down the list.
4. Category order is the same as in the Live TV tab.
5. Holding Up/Down in the channel list: no jumps in row height, no stutter when the program arrives.
6. Favorites empty: the focus stays usable (Back, Left, Right work).

## Risks

- Not compiled. Compose details (for example `AsyncImage(onError = …)`, `liveHoldThrottle` as `internal`) were checked against the code in the repository, not by a build.
- The 320 ms pause is a guess; change `ChannelMenuLogic.PREVIEW_DELAY_MS` if it feels slow.
- `PlaylistStore.catOrder` is read as a remember key; if it is not snapshot state, a changed provider order shows on the next opening (unverified).

## Test results

- Ran on the JVM: `ChannelMenuLogicTest` 6 of 6.
- Syntax check (parse only) of `PlayerLive.kt`: no syntax errors. Not compiled against Android/Compose.
- Not run: Gradle build, device test.

## Test steps for the maintainer (Fire TV remote)

1. Play a live channel, press Left: the channel list opens on the current channel with numbers and logos (or letters).
2. Left again: categories open with the focus on the current category. Hold Down: smooth, the list on the right changes only when you stop.
3. Press OK on a category: its channels, focus on the first one. Repeat with Right instead of OK.
4. Hold Down in the channel list for 5 s, then Up: no jumps, no lag.
5. Long OK on a channel: favorite toggles (or the menu with own lists). Favorites category with no favorites: the line "Keine Sender in dieser Auswahl" has the focus; Left and Back still work.
6. Hide the category of the current channel (Live TV tab), open the menu, Left: the focus is on "Alle Sender".
7. Settings > Live TV > Senderzeilen "Klein"/"Groß": the rows in the menu change size.

## Rollback

Revert the commit (`PlayerLive.kt`, `ChannelMenuLogic.kt`, `DictQ.kt` row).

## Status

Implemented, not committed by the maintainer yet, not built, not tested on a device.
