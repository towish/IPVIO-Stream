# T-029 to T-035 "Next level": Bell numbers, quick fixes, DubTvP identity, avant-garde

Requested and approved by the maintainer on 22 Nov 2026 ("implement the four proposals, do not touch the web sources").
The web sources (`WebSources.kt`, `WebPlayer.kt`, `WebExtract.kt`) were not changed.

## T-029 Bell channel numbers on the remote

- `QcLineupLogic.sortNumbered`: with the Québec order on, the first version of a recognised channel gets its Bell Fibe
  number (TVA 103, RDS 108); every other channel, including second regional versions, gets a number from 1000 up in list
  order. Without any recognised channel the numbers stay the positions 1, 2, 3 … (as before).
- `QcOrder`: keeps the numbering with the sorted list (`numbers`, `byNumber`); plain lists get positions.
- Live TV (`LiveTab.kt`), the player channel menu (`PlayerLive.kt`) and the guide (`PlayerGuide.kt`) show the same
  numbers; the player banner too.
- `PlayerScreen.kt` `commitDigits`: a typed number opens the channel with that number (before: the position in the
  current list, which differed per category); up to 5 digits; the digit box names the channel it will open.
- Visible change: channel numbers change for users with the Québec order on.

## T-030 Quick fixes

- `Trending.kt`: TMDB trends in the app language plus English (before: always German), cached per language.
- Logo letters "TVA" instead of "CA" in Live TV, home cards, player panels (`ChannelMenuLogic.monogram`); the Live TV
  logo badge falls back to letters when the logo does not load.
- `Billboard.kt`: the home info text fades out, then in (no more two titles on top of each other); picture crossfade
  300 ms instead of 600 ms.
- Held Up/Down: one shared throttle (`liveHoldThrottle`) for Live TV, the channel menu and the guide; it allows about 9,
  then 16, then 28 steps per second the longer the key is held (`ChannelMenuLogic.holdGapMs`).

## T-031 DubTvP identity in the app

- Logo chosen by the maintainer after several rounds: "D verre rouge" (a red glass D whose counter is the play sign).
  The first procedural renders did not reach the quality he wanted, so the mark is now rendered in Blender (Cycles):
  ruby glass coloured by absorption, a hidden backlight that only the glass sees, polished rims and one glint.
  Sources in `docs/brand/`: `dubtvp_mark_scene.py` (the scene; defaults = the chosen look) and `compose_assets.py`
  (icon, foreground, legacy icons and banner from the transparent render). Both reproduce the committed files exactly.
- Wordmark and texture (maintainer: "logo ok, improve the font, add textures"): the name "DubTvP" is set in Sora
  SemiBold (SIL OFL 1.1) and rendered in Blender as brushed aluminium with polished edges
  (`docs/brand/dubtvp_wordmark_scene.py`). The banner and the icon background are dark spun titanium (fine concentric
  lathe marks, the radial highlight of spun metal, red light from the glass), made in `compose_assets.py`. The D is
  unchanged. The font only serves to render the pictures; the app does not ship it.
- Icon background: `drawable/ic_launcher_bg_dubtvp.xml` is now a `<bitmap>` wrapper around
  `drawable-nodpi/ic_launcher_bg_dubtvp_spun.webp` (432 px), so the adaptive-icon files stay as they were.
- `Brand.kt`: `BrandMark` shows `drawable-nodpi/ic_brand_mark_3d.webp`; `BrandLoader` adds a breathing red glow and a
  light band drawn with `BlendMode.SrcAtop` on an offscreen layer, so the band stays inside the picture's own pixels
  (draw phase only). The flat path `BrandArt` is gone: the 3D render is seen at an angle and no longer matches it.
  Used on the playlist loading screen and for buffering in the player.
- App icon: new adaptive layers `ic_launcher_bg_dubtvp.xml`, `drawable-nodpi/ic_launcher_fg_dubtvp.webp`,
  `ic_launcher_mono_dubtvp.xml` (themed icons); legacy `mipmap-*/ic_launcher*.webp` re-rendered; TV banner
  `drawable/banner.png` (640 × 360). The old `ic_brand_mark.png`, `ic_logo_fg.png`, `ic_launcher_foreground.xml` and
  `ic_launcher_background.xml` are no longer used (can be deleted).
- App name: `DubTvP` (release) and `DubTvP Test` (debug) in `app/build.gradle.kts`, `strings.xml`. The application ID
  `com.ipvio.stream` and all `ipvio_*` storage names stay. In-app texts that say "IPVIO Stream" (German dictionary keys)
  are unchanged.
- `Look.kt`: the default color theme (index 0) is now the DubTvP red (#E3143A / #FF6B7D) instead of the IPVIO green;
  users on the default see red; green stays available as "Smaragd". Revert = one line.
- Home tiles and settings tiles: neutral surfaces with the icon in the accent color (red for Live TV, gold for favorites)
  instead of one bright color per tile.

## T-032 Ambient color (home)

- `AmbientLogic.dominant` (pure): dominant color of a 16 × 9 copy of the picture, toned down for text.
- `Billboard.kt`: the backdrop gradients take 22 % of that color and change softly (700 ms) with the selected title.

## T-033 "Tonight" row (home)

- `TonightLogic` (pure): the evening window (19:00–23:00, or from now until 23:30, or the next 3 hours) and one show per
  channel (at least 20 minutes, 30 preferred; a show that began less than 15 minutes ago counts).
- `HomeScreen.kt`: block "tonight" ("Heute Abend") for the favorite, recent and first channels (max. 14), from
  `EpgFull` 1.5 s after the home screen; `HomeParts.kt` `TonightRow` / `TonightCard` (OK opens the channel, menu key or
  long OK sets a reminder for a show that has not started). `HomeLayout.kt`: new block (existing installs get it at the
  end of their order; it can be moved or hidden in the home layout settings).

## T-034 Google TV "Watch Next" — not implemented

Publishing to the Watch Next ("Continue watching") row on Google TV requires Google's prior approval through a
certification process, and Fire TV does not show it. A sideloaded open-source app would not appear there, so nothing
was built and no dependency or permission was added.

## T-035 Cinema mode (player)

- Holding Back in the live player switches cinema mode: no channel banner on zapping, only a discreet clock at the bottom
  right. Holding Back again leaves it; a short Back works as before. Toast "Kinomodus an / aus".

## Strings

`DictQ.kt`: "Kinomodus an", "Kinomodus aus", "Was heute Abend auf deinen Sendern läuft" (10 languages). "Heute Abend"
already existed.

## Test results

- Ran on the JVM (Kotlin 2.2.10, stand-in for JUnit): 42 of 42 — `QcLineupLogicTest` 13, `ChannelMenuLogicTest` 7,
  `GuideGridLogicTest` 6, `AmbientLogicTest` 3, `TonightLogicTest` 4, `DictConsistencyTest` 3, `SettingsResetLogicTest` 6.
- Parse-only syntax check of all changed Kotlin files: no syntax errors. Not compiled against Android/Compose.
- Not run: Gradle build, device test.

## Test steps for the maintainer

1. Build and install over the current app: the launcher shows "DubTvP" with the red glass D on dark spun metal
   (round and square launchers); Android TV home shows the new banner with the metal "DubTvP"; the playlist loading screen and buffering show the mark with a band of light that stays inside the D
   (no light outside its edges or in the play hole).
2. Live TV with the Québec order on: TVA shows 103 everywhere; in the player type 1-0-3: the box shows "TVA …", then
   TVA opens. Type a number from 1000 up: the matching other channel opens.
3. Hold Down in Live TV for 3 s: the list speeds up after about a second.
4. Home: move along the rows: the background takes a hint of each picture's color; the info text no longer overlaps.
5. Home: "Heute Abend" row (at the end for existing installs): shows of tonight; long OK on a later show sets a reminder.
6. Player: hold Back: "Mode cinéma activé", only a small clock; zap: no banner; hold Back again: off. Short Back still
   closes the player.
7. Settings > Design: default theme is red; "Smaragd" brings back green.

## Open

- Logo: the Blender render (22 Nov 2026) replaces the procedural one; the maintainer has not judged it on the device yet.
- The monochrome layer `ic_launcher_mono_dubtvp.xml` (themed icons) is the flat D, seen from the front; the colour
  icon shows the D at a slight angle. This is usual for themed icons.
- Not checked: `AsyncImage(onSuccess)` copy of hardware bitmaps on old Fire TV sticks (falls back to no ambient color).

## Rollback

Revert the commit. Stored data is unchanged (no new storage keys besides the T-026 ones).

## Status

Implemented in the working tree, not committed by the maintainer yet, not built, not tested on a device.
