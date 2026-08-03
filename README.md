# Insulin Calculator App

An Android app to calculate insulin dose based on blood glucose and carbohydrate intake, built with Jetpack Compose.

## Features

### Dose Calculator
- Sliders for current blood glucose (mmol/L, 0.1 increments), carbs (g, 5g increments), correction dose, target BG, and ICR
- Live calculation of total insulin dose
- Scrollable layout that works on all screen sizes

### CGM Integration (LibreLinkUp)
- Fetch your current glucose reading directly from your Libre sensor via the LibreLinkUp API
- Tap the refresh icon next to the BG slider to populate it with your latest reading
- Link your LibreLinkUp account in Settings (email + password — credentials are not stored, only the auth token)
- Region selector: EU, US, AU, CA, DE, JP (defaults to EU)

### Dose History
- Full log of saved doses with timestamp, BG, carbs, correction, target, ICR, and final dose
- Sortable columns, date filter, and paginated view (10 entries per page)
- Long-press any entry to delete it
- Estimated daily basal calculation from history

### Navigation
- Fixed bottom navigation bar: Calculator / History / Settings

## Parameter Definitions

- **Target BG:** The blood glucose value (mmol/L) you aim to reach after dosing. Typical range: 4.0–8.0 mmol/L.
- **Correction Dose:** A multiplier applied to the carb coverage calculation.
- **ICR (Insulin-to-Carbohydrate Ratio):** Grams of carbohydrate covered by 1 unit of insulin. E.g. ICR 10 = 1 unit per 10g carbs.

## Setup Instructions

1. **Clone the repository**
2. **Open in Android Studio** (Giraffe or newer recommended)
3. **Build the project** — Gradle will download all dependencies
4. **Run on an emulator or device** (Android API 24+)

### LibreLinkUp setup

1. Open the app and go to **Settings**
2. Select your region (EU for Switzerland)
3. Enter your LibreLinkUp email and password and tap **Link Account**
4. Return to the Calculator screen and tap the refresh icon next to the BG slider

## Project Structure

```
app/src/main/java/com/example/insulincalculator/
├── data/
│   ├── InsulinEntry.kt          — data model
│   ├── InsulinRepository.kt     — SharedPreferences storage
│   └── LibreLinkUpRepository.kt — LibreLinkUp API + token storage
├── ui/
│   ├── InsulinCalculatorViewModel.kt
│   ├── HistoryViewModel.kt
│   ├── SettingsViewModel.kt
│   ├── ViewModelFactories.kt
│   └── theme/
└── MainActivity.kt              — all composable screens
```

## Requirements

- Android Studio Giraffe or newer
- Android SDK 34, minSdk 24
- Kotlin 1.9.0+

## License

MIT
