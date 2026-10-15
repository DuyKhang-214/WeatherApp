# Weather Forecast (Android, Java)

Firebase Auth login/register/logout, OpenWeather current + hourly + 5-day forecast, city search with
suggestions, precipitation/cloud/temp/wind map, RSS reader, profile, Lottie weather animations.

## Setup (4 steps)
1. **Open** this folder in Android Studio (Hedgehog or newer, JDK 17). Let Gradle sync.
2. **OpenWeather key** - create a free key at openweathermap.org/api, then add to `local.properties`:
   `OWM_KEY=your_key`  (new keys can take up to ~2 hours to activate)
3. **Maps key** - enable "Maps SDK for Android" in Google Cloud, then add to `local.properties`:
   `MAPS_API_KEY=your_key`
4. **Firebase** - create a project, add an Android app with package `com.example.weatherapp`,
   enable *Authentication > Email/Password*, and **replace `app/google-services.json`**
   with the file you download (the one included is a placeholder so the project compiles).

Run on an emulator with Google Play services (or a real phone).

## Requirement checklist
| Requirement | Where |
|---|---|
| Options Menu / SubMenu | `menu_main.xml` (Units submenu), `MainActivity` |
| ContextMenu | long-press a favorite city chip - `WeatherFragment` |
| Navigation bar | `BottomNavigationView`, `menu_bottom.xml` |
| UI via visual tool / XML | `activity_login.xml` (Layout Editor), all other layouts |
| UI via Java code | `RegisterActivity` (whole screen), hourly cards in `WeatherFragment` |
| Firebase Auth login / register / logout | `LoginActivity`, `RegisterActivity`, `MainActivity.confirmLogout` |
| Profile screen | `ProfileFragment` (name, email, verified, dates, edit name) |
| RSS reader | `RssFragment`, `RssParser` |
| Weather API | `WeatherApi` (Retrofit) |
| Intents | explicit: Login -> Register/Main; implicit: ACTION_VIEW (RSS), ACTION_SEND (share) |
| Search bar | `SearchView` + OpenWeather geocoding suggestions (debounced) |
| Precipitation map | `MapFragment` (OpenWeather tiles over Google Maps, tap for point weather) |
| Lottie | `res/raw/anim_*.json` swapped by condition + day/night |
| Custom features | GPS location, favorites, default city, unit switch, shake-to-refresh, pull-to-refresh, 10-min auto-refresh, gradient themes, cross-fade animations |

`app/google-services.json` is a placeholder. `gradle-wrapper.jar` is not included; Android Studio
downloads Gradle from `gradle-wrapper.properties` on sync (or run `gradle wrapper` once).
