# ssui-mobile

Android client for the **SSUI (Server Side UI)** proof of concept. The app knows how to render five
primitive components (`COLUMN`, `ROW`, `TEXT`, `BUTTON`, `IMAGE`); the backend decides which ones appear,
in what order and with what content. See `../main-idea.md` for the full spec.

## Stack

| Item | Choice |
|------|--------|
| Language | Kotlin 2.4 (Compose compiler Gradle plugin, kotlinx.serialization plugin) |
| Build | AGP 8.13, Gradle 8.14 (wrapper included), Kotlin DSL, version catalog in `gradle/libs.versions.toml` |
| SDK | minSdk 26, compileSdk = targetSdk = 36 |
| UI | Jetpack Compose, Material 3 (Compose BOM) |
| Networking | Ktor Client (OkHttp engine) + content negotiation + kotlinx.serialization JSON |
| Images | Coil 3 (`coil-compose` + `coil-network-okhttp`) |
| DI | Koin (`koin-android`, `koin-androidx-compose`) |
| Architecture | MVVM + MVI (`ScreenUiState` / `ScreenEvent` / `ScreenEffect`) |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine |

## Project layout

```
app/src/main/kotlin/com/ssui/mobile
├── SsuiApp.kt                 # Application: starts Koin, configures Coil
├── MainActivity.kt
├── di/                        # Koin module + logcat Logger
├── data/
│   ├── remote/                # Ktor client factory, ScreenApi, JSON config, DTOs
│   ├── mapper/                # ScreenMapper: DTO -> domain (validation + defaults)
│   └── ScreenRepositoryImpl.kt
├── domain/                    # Screen, UiElement, Action, Padding, Size, enums, ScreenRepository, Logger
└── ui/
    ├── screen/                # ScreenViewModel, MVI contract, ScreenRoute (Loading / Error / Success)
    ├── render/                # RenderElement + SsuiColumn / SsuiRow / SsuiText / SsuiButton / SsuiImage
    └── theme/
```

## Prerequisites

- JDK 17 or newer (JDK 21 recommended).
- Android SDK with platform `android-36` (Android Studio installs it on demand).
- A `local.properties` file at the repo root pointing at the SDK. It is git-ignored; create it if missing:

  ```properties
  sdk.dir=/Users/<you>/Library/Android/sdk
  ```

## Running the backend

The app expects the `ssui-backend` service (sibling repo) to be reachable from the emulator.
Start it with `docker compose up --build` in `../ssui-backend`; it listens on host port `8080` by default.

## Backend URL (`BASE_URL`)

The app reads the backend address from `BuildConfig.BASE_URL`. The committed default, per the spec, is

```
http://10.0.2.2:8080
```

`10.0.2.2` is the Android Emulator's alias for the host machine's loopback interface, so this works
out of the box when the backend runs on your laptop and the app runs in the emulator.

Ways to change it:

1. **One-off build, no file changes** - pass a Gradle property:

   ```bash
   ./gradlew :app:assembleDebug -PbaseUrl=http://10.0.2.2:8081
   ```

   (Also usable in Android Studio: *Settings > Build, Execution, Deployment > Compiler > Command-line options*
   `-PbaseUrl=...`.)

2. **Persistent change** - edit `defaultBaseUrl` in `app/build.gradle.kts`.

3. **Physical device** - use your laptop's LAN IP, e.g. `-PbaseUrl=http://192.168.1.20:8080`.
   Cleartext HTTP is already allowed via `android:usesCleartextTraffic="true"` (POC only).

The screen loaded at startup is hardcoded to `home`
(`ScreenViewModel.DEFAULT_SCREEN_NAME`); the request is `GET {BASE_URL}/api/v1/screens/home`.

## Build, test, run

```bash
# build the debug APK
./gradlew :app:assembleDebug

# run the JVM unit tests (mapper, DTO parsing, ViewModel)
./gradlew :app:testDebugUnitTest      # or simply: ./gradlew test
```

### Emulator

Any AVD with API 34+ works. From the command line:

```bash
# list available AVDs, then boot one headlessly
~/Library/Android/sdk/emulator/emulator -list-avds
~/Library/Android/sdk/emulator/emulator -avd <name> -no-snapshot -no-audio -no-boot-anim &
adb wait-for-device
adb shell getprop sys.boot_completed     # wait until it prints 1

# install and launch
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ssui.mobile/.MainActivity
```

Or open the project in Android Studio and press *Run*.

### What you should see

1. A centred loading spinner.
2. The `home` screen from the backend: a grey column with a blue-grey row (avatar + white
   "Welcome to SSUI"), a wide landscape image, a body text and a "Tap me" button.
3. Tapping the button shows a toast with the text that came from the server.
4. With the backend down you get an error message and a **Retry** button; start the backend and tap
   Retry to render the screen.
5. The top app bar has a **refresh** icon. Edit the screen on the server (see
   [Editing the UI](#editing-the-ui-from-the-server)), tap refresh, and the new UI renders
   without relaunching or rebuilding the app.

## Editing the UI from the server

The app only knows how to render the primitives `COLUMN`, `ROW`, `TEXT`, `BUTTON` and `IMAGE`.
Everything else (order, texts, colours, sizes, actions) comes from the `home` document in MongoDB, so
you change the UI by changing that document and tapping refresh in the app. Three ways to do it:

1. **REST API (recommended)**, using the backend's `PUT /api/v1/screens/home`. Fetch, edit, put back:

   ```bash
   BASE=http://localhost:8080/api/v1
   curl -s $BASE/screens/home > home.json
   # edit home.json in your editor (e.g. change a textContent, a containerColor, add a child), then:
   curl -s -X PUT -H 'Content-Type: application/json' --data @home.json $BASE/screens/home
   ```

   Or a one-liner with `jq` that changes the button label:

   ```bash
   curl -s $BASE/screens/home \
     | jq '.root.children[3].textContent = "Refreshed!"' \
     | curl -s -X PUT -H 'Content-Type: application/json' -d @- $BASE/screens/home
   ```

2. **mongosh** against the exposed Mongo port (or inside the container if mongosh is not installed):

   ```bash
   mongosh mongodb://localhost:27017/ssui --eval \
     'db.screens.updateOne({name:"home"}, {$set: {"root.children.3.textContent": "Hello from mongosh"}})'
   # same thing without a local mongosh:
   docker exec ssui-mongo mongosh --quiet mongodb://localhost:27017/ssui --eval \
     'db.screens.updateOne({name:"home"}, {$set: {"root.children.3.textContent": "Hello from mongosh"}})'
   ```

3. **MongoDB Compass**: connect to `mongodb://localhost:27017`, open `ssui > screens`, edit the `home`
   document in place and save.

Then tap the refresh icon in the app. The element schema (all fields, enums, sizes, actions) is
documented in the spec `../main-idea.md`, section 4. Invalid documents are rejected by the API with a
`400` and a message; if you edit directly in Mongo and break the shape, the app drops the broken element
and logs it under the `ScreenMapper` / `SsuiDto` tags.

Useful logcat filter while testing:

```bash
adb logcat -s ScreenViewModel ScreenMapper SsuiDto SsuiColor AndroidRuntime
```

## Robustness rules implemented

- Unknown JSON fields are ignored (`ignoreUnknownKeys = true`).
- Enum-like fields are deserialized as plain strings; the mapper converts them. An element with an
  unknown `type`, an unknown alignment/arrangement, an invalid `Size` (`FIXED` without `dp > 0`) or
  negative padding is **dropped and logged**; its siblings still render.
- A child whose JSON is structurally invalid (e.g. a string where an int is expected) is dropped and
  logged by `LenientUiElementListSerializer`; the rest of the tree parses.
- An unknown **action** type is logged and ignored; the element itself is still rendered (without `onClick`).
- Invalid colour strings fall back to the Material theme colour.

## Troubleshooting

- `adb install` fails with `Requested internal only, but not enough space`: the AVD's `/data`
  partition is full. Either free space on it or create a new AVD (any API 34+ image) with a larger
  internal storage; the app itself needs ~40 MB.
- Error state `Failed to connect to /10.0.2.2:8080`: the backend is not running on the host, or you are
  on a physical device (use the LAN IP, see "Backend URL").
