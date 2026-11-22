# T-037 — Full TMDB search in the search tab

Requested by the user (2026-11-22): a title that is on Streambert (e.g. South Park) could
not be found in the app, because the app only searches the provider's catalogue and the
web sources hang on detail pages of titles that exist there. Streambert browses TMDB
directly — this task gives the app the same reach.

## What was built

- `TmdbSearch.kt`
  - `TmdbSearch.search(query)`: TMDB `search/multi` (app language, no adult results),
    movies and series only, poster required, sorted by popularity, max 20.
  - Hits become synthetic items `nr:tmdb:movie:<id>` / `nr:tmdb:tv:<id>` — the existing
    info-only scheme, so the detail page, `WebSources.tmdbIdOf` and the web player work
    unchanged. The poster URL rides in `Item.logo` so cards render before metadata loads.
  - `TmdbEpisodePicker`: season chips + episode rows for a TMDB series (seasons via
    `MetaRepo.tvSeasons`, episode names via `MetaRepo.season`, plain numbered rows as
    fallback from the season's episode count). Season 0 (specials) is skipped — the
    web-source URL scheme cannot address it. Picking an episode opens the existing
    `WebSourcePicker` with a `WebSources.Target`.
- `SearchScreen.kt`: debounced (400 ms) TMDB section under the playlist hits — rows
  "TMDB · Filme" / "TMDB · Serien", following the Filme/Serien/Live TV filters; the
  "no hits" state waits for the TMDB answer; focus and the info-panel preview include
  the TMDB rows.
- `DetailScreen.kt`: on info-only items (new releases and TMDB hits), a series now also
  gets a "Web-Quellen" button that opens the episode picker — this also unlocks web
  sources for series from the streaming rails (e.g. a Netflix series), which previously
  only had the "watch on the service" button.
- `DictB`: "TMDB · Filme", "TMDB · Serien" (FR: "TMDB · Films", "TMDB · Séries").
- Tests: `TmdbSearchTest` (parsing: media types, poster/title filtering, popularity
  order, id scheme round-trip through `WebSources.tmdbIdOf`, garbage input).

## Notes

- Needs a TMDB key (`MetaRepo.enabled()`); without one the section stays empty, like
  the new-release rails.
- Metadata on the detail page still resolves by title match (same as the rails); the
  web sources always use the exact TMDB id from the search hit.
