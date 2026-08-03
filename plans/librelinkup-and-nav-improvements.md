# LibreLinkUp Integration & Navigation Improvements

## Objective

Add a fixed bottom navigation bar (Calculator / History / Settings), make the calculator screen scrollable, and integrate the unofficial LibreLinkUp API so the user can fetch their current CGM glucose reading directly into the BG slider. A new Settings screen allows the user to link/unlink their LibreLink account and choose their regional API endpoint.

## Current State

- Two screens connected via simple button navigation: "View History" button on Calculator, "Back" button on History.
- Calculator is a non-scrollable `Column`.
- No networking; all data in `SharedPreferences` via Gson.
- Dependencies: Compose Material 1.5.0, Gson 2.10.1, Navigation Compose 2.7.7.

## Proposed Approach

### Phase 1 — Bottom Navigation Bar
Wrap the `NavHost` in a `Scaffold` with `BottomNavigation` (three tabs: Calculator, History, Settings). Remove the "View History" / "Back" buttons. All nav is tab-based with `popUpTo + saveState`.

### Phase 2 — Scrollable Calculator Screen
Add `Modifier.verticalScroll(rememberScrollState())` to the calculator `Column`. Replace `Spacer(weight(1f))` (incompatible with scroll) with a fixed-height spacer.

### Phase 3 — LibreLinkUp API Integration
- Add `okhttp3:okhttp:4.12.0` dependency and `INTERNET` permission.
- New `LibreLinkUpRepository` uses OkHttp directly (avoids dynamic base-URL problem with Retrofit).
  - Endpoints: `POST /llu/auth/login`, `GET /llu/connections`.
  - Handles the LibreLink redirect flow (login may return `data.redirect=true` with a new region).
  - Stores token + expiry (never the password) in a dedicated `librelinkup_prefs` SharedPreferences file.
  - `isLinked()` checks both token presence and expiry timestamp.
  - Glucose unit conversion: `GlucoseUnits=1` (mg/dL) → ÷18.0182 → mmol/L.
- `InsulinCalculatorViewModel` gains `fetchGlucoseFromLibre()` and two new state fields: `isLoadingGlucose`, `glucoseError`.
- Calculator screen shows a small Refresh `IconButton` next to the BG label; loading spinner replaces it during fetch.

### Phase 4 — Settings Screen
- `SettingsViewModel` / `SettingsState` manage the link flow.
- Linked state: shows connected email + "Unlink" button.
- Unlinked state: email + password fields, region dropdown (EU/US/AU/CA/DE/JP), "Link Account" button.
- If a stored token has expired, the email is pre-filled and a warning is shown.

### Region List

| Key | Endpoint |
|-----|----------|
| EU  | api-eu.libreview.io |
| US  | api-us.libreview.io |
| AU  | api-au.libreview.io |
| CA  | api-ca.libreview.io |
| DE  | api-de.libreview.io |
| JP  | api-jp.libreview.io |

## Tasks

- [x] Create plan file
- [x] Add OkHttp dependency to `app/build.gradle`
- [x] Add `INTERNET` permission to `AndroidManifest.xml`
- [x] Create `data/LibreLinkUpRepository.kt`
- [x] Create `ui/SettingsViewModel.kt`
- [x] Update `ui/ViewModelFactories.kt` (add `SettingsViewModelFactory`, update `InsulinCalculatorViewModelFactory`)
- [x] Update `ui/InsulinCalculatorViewModel.kt` (add glucose fetch + new state fields)
- [x] Refactor `MainActivity.kt`:
  - [x] Bottom navigation scaffold
  - [x] Scrollable calculator screen with Refresh button
  - [x] History screen without Back button
  - [x] Settings screen composable

## Risks / Unknowns

- The LibreLinkUp API is unofficial/reverse-engineered and may change without notice.
- API `expires` field is a Unix timestamp in seconds; if the LibreLink server returns a different format this check could fail silently (token will be used past expiry and a 401 will trigger re-link prompt).
- LibreLink accounts in Switzerland use the EU endpoint, but redirect handling ensures correct endpoint is used even if the user selects the wrong region.

## Notes

- Password is never persisted — only the auth token.
- `Dispatchers.IO` used for all OkHttp calls.
- The `performLogin` helper is synchronous/blocking (called inside `withContext(Dispatchers.IO)`).
- Using `org.json.JSONObject` (built into Android) rather than adding a JSON library.
