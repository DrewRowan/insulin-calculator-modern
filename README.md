# Insulin Calculator App

A simple Android app to calculate insulin dose based on blood glucose and carbohydrate intake, using Jetpack Compose.

## Features
- Sliders for current blood glucose (mmol/L, increments of 0.1) and carbs in food (grams, increments of 5)
- Dropdowns for Target BG and ISF
- Input field for ICR
- Live calculation of total insulin dose

## Parameter Definitions
- **Target BG (Target Blood Glucose):** The blood glucose value (in mmol/L) you aim to reach after insulin dosing. Typical values are between 4.0 and 8.0 mmol/L.
- **ISF (Insulin Sensitivity Factor):** The amount (in mmol/L) that 1 unit of insulin is expected to lower your blood glucose. Typical values range from 5 to 40 mmol/L per unit.
- **ICR (Insulin-to-Carbohydrate Ratio):** The number of grams of carbohydrate that 1 unit of insulin will cover. For example, an ICR of 10 means 1 unit of insulin covers 10 grams of carbs.

## Setup Instructions

1. **Clone the repository**
2. **Open in Android Studio** (Giraffe or newer recommended)
3. **Build the project** (Gradle will download dependencies)
4. **Run on an emulator or device**

## Project Structure
- `app/src/main/java/com/example/insulincalculator/MainActivity.kt`: Main UI and logic
- `app/build.gradle`: App-level dependencies (Jetpack Compose)
- `build.gradle`: Project-level Gradle config

## Requirements
- Android Studio (Giraffe or newer)
- Android SDK 34+
- Kotlin 1.9.0+

## License
MIT 