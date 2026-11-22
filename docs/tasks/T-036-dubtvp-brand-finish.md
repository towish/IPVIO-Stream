# T-036 DubTvP brand finish: texts, old logos, start intro, design system

Requested by the maintainer on 22 Nov 2026 ("termine la mise à jour de tous les points"), all four points chosen:
IPVIO texts to DubTvP, remove the old logos, start intro, design system up to date. Follows T-031 (task file
`T-029-T-035-next-level.md`).

## Goal

The app no longer shows the old name or logo anywhere a user can see it, opens with a short DubTvP intro, and the
brand is recorded in one design system.

## Changes

### User-visible texts "IPVIO" → "DubTvP"

- German keys changed in the code and in the dictionaries together, with the grammar of each language (Turkish
  suffixes, Croatian/Serbian case endings, French "de DubTvP"):
  `SettingsHub.kt` (about, donate, autostart, permission hint, diagnostics clipboard label, version line, player
  and licence hints), `AddPlaylistScreen.kt` (the metal wordmark picture next to the mark instead of the text "IPVIO", "only a player" box, backup hint),
  `Boomer.kt` (autostart hint), `Donate.kt` (English hint), `ImportFiles.kt` (version line of the exported report).
  Dictionaries: `DictB`, `DictC`, `DictD`, `DictE`, `DictI`, `DictJ`, `DictNl2`, `DictNl3`, `DictNl5`.
- Background preset "IPVIO Navy" is now "Marineblau" (Navy blue, Bleu marine …). Presets are stored by index, so a
  user's choice is kept.
- Phone pages served by the app (`PhoneImport.kt`, `RemoteLogic.kt`): page title and heading "DubTvP". Routes and
  parameters are unchanged.
- Kept on purpose (technical identifiers, not shown in the UI): HTTP User-Agent strings (`IPVIO-Stream`,
  `IPVIO v1.2` for the subtitle API, `IPVIOStream/1.0`), the log tag `IPVIO-Sub`, the Google Drive folder name
  `IPVIO Stream Sync` and its hint (stored data), the updater's APK name match `IPVIO-Stream.apk` (any APK asset is
  still accepted as fallback), the package `com.ipvio.stream` and all `ipvio_*` preference names.

### Old logo files removed

`res/drawable/ic_brand_mark.png`, `ic_logo_fg.png`, `ic_launcher_foreground.xml`, `ic_launcher_background.xml`. No
code, manifest or resource refers to them (checked by search over `app/`).

### Start intro (new)

- `BrandIntroLogic.kt` (pure, tested): 3.3 s timeline and `shouldShow` (fresh start only, switch on, animations
  not "Aus", not when a reminder opens a channel).
- `Brand.kt`: `BrandIntro` composable above the whole app: spun-titanium backdrop, the glass D turning into place
  (`rotationY` −34° → 0°) with a band of light, then the brushed-metal wordmark sliding in with its own band; it
  ends on the TV banner layout and fades out over the app, which loads underneath. Only the draw phase changes per
  frame. `glassSweep` (offscreen layer + `BlendMode.SrcAtop`) is now shared with `BrandLoader`.
- `MainActivity.kt`: decides on a fresh start; `dispatchKeyEvent` ends the intro on any key and swallows the rest
  of that key press (repeats and release), so it cannot trigger anything underneath; Back does not leave the app.
  A tap also ends it.
- Setting "Intro beim Start" in Design > Animationen (`General.introAnim`, key `intro_anim` in `ipvio_general`,
  default on; reset with the Design page). Two new rows in `DictQ.kt` (10 languages).
- New pictures (`res/drawable-nodpi/`): `intro_backdrop_spun.webp` (1280 × 720, 19 KB) and
  `brand_wordmark_metal.webp` (1112 × 240, 43 KB). The adaptive icon background is now a lossy WebP (5 KB instead
  of 181 KB). All made by `docs/brand/compose_assets.py`.

### Design system

The Design System artifact "DubTvP — Identité visuelle" (claude.ai, private to the maintainer) holds the brand
book (French), tokens measured from the app and the renders, the fonts (Outfit, Inter, Sora SemiBold), the logos,
app icons, materials and both intro videos.

## Test results

- Ran on the JVM (Kotlin 2.2.10, stand-in for JUnit): 47 of 47 — `BrandIntroLogicTest` 5 (new),
  `DictConsistencyTest` 3, `SettingsResetLogicTest` 6, `QcLineupLogicTest` 13, `ChannelMenuLogicTest` 7,
  `GuideGridLogicTest` 6, `AmbientLogicTest` 3, `TonightLogicTest` 4.
- The renamed keys were looked up through `I18nCore` in all 10 languages: every one translates.
- Parse-only syntax check of the changed Kotlin files: no syntax errors. Not compiled against Android/Compose.
- The intro was simulated frame by frame in Python with the same timeline and layout (preview video, not a device
  recording).
- Not run: Gradle build, device test.

## Test steps for the maintainer

1. Build and install. Start the app from the launcher: the intro plays (about 3 s) and ends on the home screen.
   Start it again: it plays again. Press any key during the intro: it stops at once and that key does nothing
   else (Back does not close the app).
2. Settings > Design > Animationen: switch "Intro beim Start" off, restart: no intro. Set animations to "Aus": no
   intro either.
3. Open a reminder notification: the channel opens without the intro.
4. Settings > About, Donate, General (autostart), diagnostics copy, Add playlist screen, phone page title: all say
   DubTvP, in German, English and French at least.
5. Settings > Design > background: the first preset reads "Marineblau" / "Navy blue" / "Bleu marine" and stays
   selected if it was before.
6. On an old Fire TV stick: the intro runs smoothly; if it stutters, note it (the two light bands use offscreen
   layers).

## Risks

- Visible change for all users: an intro at every launch from the launcher (default on, can be switched off).
- The intro covers the first 3 s; "resume last channel at start" may start playback underneath (sound before the
  picture shows).
- The four deleted files must also be deleted in the working tree (the assistant cannot delete files there; see
  the delivery note).

## Rollback

Revert the commit. The new preference key `intro_anim` is simply ignored by older versions.
