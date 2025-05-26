# Insulin Calculator App

A simple Android app to calculate insulin dose based on blood glucose and carbohydrate intake, using Jetpack Compose.

## Features
- Sliders for current blood glucose (mmol/L, increments of 0.1) and carbs in food (grams, increments of 5)
- Input fields for Target BG, ISF, and ICR
- Live calculation of total insulin dose

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
- Android SDK 33+
- Kotlin 1.8+

## License
MIT 