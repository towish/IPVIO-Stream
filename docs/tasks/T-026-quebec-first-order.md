# T-026 Live TV: Québec channels first, in the Bell Fibe order

## Goal

Maintainer request (22 Nov 2026): the Québec channels come first in Live TV, in an order like Bell. Approved: the order is on by default, it can be switched off (back to the provider order), and the preferred region for local stations is Québec City.

## Findings

- The live list had the provider order everywhere: `HomeScreen.buildHome` (Live TV tab), `MainActivity` `liveAll` (player, zapping, channel menu, guide). The channel number is the position in that list (`LiveTab.kt` `numOf`, guide).
- `GuideCollectionLogic.pinnedFirst` already put Canadian / Québec groups first in the guide, but only the groups, not the channels.
- Provider names vary a lot: "CA: TVA MONTREAL FHD", "QC| Noovo HD", "TVA Sports 2 (CA)", "ICI Télé Québec ᴴᴰ", call letters such as "CFCM-DT".

## Change

- `QcLineupLogic.kt` (new, pure): the Bell Fibe Québec line-up (French 101-168, English Canadian 201-205, US networks 220-224; source: Bell channel PDF `4-Program_Channel_FIBE_QUE_F.pdf`, February 2026), name cleaning (provider prefix, quality tags, region words, call letters) and a stable sort. Matching is strict: the cleaned name must equal an alias; only a few distinctive names (for example "Canal Vie") may appear inside a longer name. "ABC News Live", "CTV News Channel", "TV5 Monde" stay out of the Bell slots. Regional versions of one station stay together: chosen region, then the national feed, then Montréal, then the others. All other channels keep the provider order after the recognised ones.
- `QcOrder.kt` (new): the sorted list, cached by list identity and setting (two slots), shared by the home screen and `MainActivity`, so the channel numbers are the same in Live TV, the channel menu and the guide. Sorting runs off the main thread; `QcOrder.groups` puts the Québec / Canadian groups first in the provider order.
- `HomeScreen.kt`: `buildHome` uses `QcOrder.live` and `QcOrder.groups`; `HomeCache` has the setting as an extra key.
- `MainActivity.kt`: `liveAll` comes from `produceState` on `Dispatchers.Default` (provider order until the sorted list is there, once per playlist).
- `General.kt`: `qcOrder` (key `qc_order`, default true) and `qcRegion` (key `qc_region`, default Québec City), both in `ipvio_general`.
- `SettingsResetLogic.kt`: both keys belong to the Live TV page reset.
- `SettingsHub.kt` (Live TV page): switch "Québec zuerst (Reihenfolge wie Bell)", region choice and a hint.
- `DictQ.kt` (new, registered in `Lang.kt` and `DictConsistencyTest.kt`): the new texts in 10 languages.

## Not changed

`Models.kt` (`Item`), the playlist cache, backup keys, favorites, channel lists, the provider category order setting (`CatOrder`: A-Z and own order still win over the Québec pinning of groups).

## Acceptance criteria

1. With a playlist that has Québec channels, Live TV > Alle Sender starts with ICI Radio-Canada Télé, Noovo, TVA, Télé-Québec, MétéoMédia, RDS … then the English Canadian and US networks, then all other channels in the provider order.
2. TVA Québec comes before TVA Montréal; with the region set to Montréal it is the other way round.
3. The channel number of a channel is the same in Live TV, the channel menu of the player and the guide; zapping follows the same order.
4. Switching the setting off restores the provider order without a restart.
5. Other playlists (no Québec channels) look exactly as before.

## Risks

- Channels with unusual names stay in the provider order (strict matching on purpose). A name that collides with an alias (a foreign channel called exactly "Z" or "MAX") would be pulled up; the US network aliases only match the bare name.
- With very large lists the first sort after loading takes a moment (20,000 names: about 60 ms on the development PC; not measured on a Fire TV). Until then the provider order shows.
- The numbers of all channels change for users who keep the default (visible change, approved).

## Test results

- Ran on the JVM (Kotlin 2.2.10 compiler, with a stand-in for JUnit; the real jar was not reachable): `QcLineupLogicTest` 10 of 10, `DictConsistencyTest` 3 of 3, `SettingsResetLogicTest` 6 of 6. Benchmark: 20,000 names sorted in about 57 ms.
- Syntax check (parse only) of the changed Android files with kotlinc: no syntax errors. They were not compiled against Android/Compose.
- Not run: Gradle build, device test.

## Test steps for the maintainer

1. Build, start with a Québec playlist. Live TV > Alle Sender: the order of acceptance criterion 1.
2. Open a TVA channel, press Left (channel menu): same numbers as in Live TV. Zap Up/Down: the next channel is the next one in the list.
3. Open the guide in the player: same numbers.
4. Settings > Live TV: switch "Québec zuerst" off: provider order again. Switch it on, choose "Montréal": TVA Montréal before TVA Québec.
5. Settings > Live TV > "Diese Seite zurücksetzen": the switch is back on, region Québec City.
6. Switch the app language to French and English: the new texts are translated.

## Rollback

Revert the commit. The two preference keys are then ignored.

## Status

Implemented, not committed by the maintainer yet, not built, not tested on a device.
