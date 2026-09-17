# SSUI - Server Side UI (POC spec)

Server Side UI (also known as Server-Driven UI) is an approach in mobile development where the app receives its UI structure (or a part of it) from a backend service. The app only knows how to render a fixed set of primitive components; the backend decides which components appear, in what order, and with what content. This lets us ship UI changes without releasing a new app version through Google Play / App Store: we just update the UI structure in the database.

## 1. Goal of the POC

Prove the end-to-end loop:

1. A screen definition (a tree of UI elements) is stored in MongoDB.
2. A Kotlin/Spring Boot backend serves it over HTTP as JSON.
3. An Android app fetches it on startup and renders it with Jetpack Compose.
4. Editing the document in MongoDB and reopening the app changes the rendered UI with no app rebuild.

## 2. Repositories and environment

One git repository (`SSUI/`) holding two independent projects side by side:

```
SSUI/
├── README.md             # overview and quick start
├── main-idea.md          # this spec
├── ssui-backend/         # Kotlin + Spring Boot + MongoDB + docker-compose (own Gradle build)
└── ssui-mobile/          # Android app, Kotlin + Jetpack Compose (own Gradle build)
```

- The two projects share nothing at build time: each has its own Gradle wrapper, `README.md` and `.gitignore`, and can be opened separately in IntelliJ IDEA / Android Studio.
- Runtime setup for the POC: the backend runs in Docker (docker compose) on the developer's laptop; the Android app runs in the Android Emulator on the same laptop.
- Therefore the app reaches the backend via the emulator's host alias: `http://10.0.2.2:8080`. Cleartext HTTP is allowed in the app via `android:usesCleartextTraffic="true"` (POC only).

## 3. Architecture overview

```
┌──────────────────┐   GET /api/v1/screens/{name}   ┌─────────────────┐        ┌──────────┐
│  Android app     │ ─────────────────────────────► │  Spring Boot     │ ─────► │ MongoDB  │
│  (Compose, MVI)  │ ◄───────────────────────────── │  backend         │ ◄───── │          │
└──────────────────┘        ScreenDto (JSON)        └─────────────────┘        └──────────┘
```

- Backend: stateless REST service, reads screen documents from MongoDB, maps entity to DTO, returns JSON.
- Mobile: fetches `ScreenDto`, maps it to a domain model, and a recursive Compose renderer draws the tree.

## 4. Data model

### 4.1 Design decisions

- One universal element model: a single `UiElement` class with a `type` discriminator and nullable type-specific fields (no polymorphic class hierarchy). Simple to store, serialize and extend.
- The whole tree of a screen is embedded in one MongoDB document (`Screen`). No flat element collection, no parent references.
- DTO and entity are separate classes on the backend, with an explicit mapper between them. Field names are identical in both for the POC.
- Colors are stored as ARGB hex strings, e.g. `"#FF5C738A"`. Readable and editable directly in the DB. `null` means "use the Material theme default".
- All sizes (padding, spacing, width, height) are in dp.
- Unknown element `type` values (e.g. added on the backend before the app supports them) are skipped silently by the app. Unknown JSON fields are ignored on both sides.

### 4.2 Screen

| Field  | Type      | Notes                                                             |
|--------|-----------|-------------------------------------------------------------------|
| id     | String    | UUID as string. Mongo `_id`.                                      |
| name   | String    | Unique, URL-safe slug used in the API path, e.g. `"home"`.        |
| root   | UiElement | Must be a `COLUMN`. Rendered inside a vertically scrollable area. |

### 4.3 UiElement

| Field                 | Type                    | Applies to      | Notes                                                                    |
|-----------------------|-------------------------|-----------------|--------------------------------------------------------------------------|
| id                    | String (UUID)           | all             | Unique within a screen.                                                  |
| type                  | enum ElementType        | all             | `COLUMN`, `ROW`, `TEXT`, `BUTTON`, `IMAGE`                               |
| children              | List<UiElement>         | COLUMN, ROW     | Empty list for leaf types (TEXT, BUTTON, IMAGE).                         |
| textContent           | String?                 | TEXT, BUTTON    | The text to show / button label.                                         |
| imageContent          | String?                 | IMAGE           | Absolute image URL. POC: public internet URLs (e.g. picsum.photos), loaded with Coil. No self-hosted images. |
| contentDescription    | String?                 | IMAGE           | Accessibility text. Optional.                                            |
| containerColor        | String? (ARGB hex)      | all             | Background color (Column/Row/Image background, Button container).       |
| contentColor          | String? (ARGB hex)      | TEXT, BUTTON    | Text color / button label color.                                         |
| padding               | Padding?                | all             | See below. `null` = no padding.                                          |
| width                 | Size?                   | all             | See below. Default: `WRAP` for leaves, `FILL` for COLUMN/ROW.            |
| height                | Size?                   | all             | See below. Default: `WRAP`. IMAGE must specify a fixed height.           |
| horizontalAlignment   | enum HorizontalAlignment? | COLUMN, ROW   | `START`, `CENTER`, `END`. Default `START`. For COLUMN: aligns children horizontally. For ROW: horizontal arrangement (`START`/`CENTER`/`END`) of children. |
| verticalArrangement   | enum VerticalArrangement? | COLUMN, ROW   | `TOP`, `CENTER`, `BOTTOM`, `SPACE_BETWEEN`, `SPACE_AROUND`, `SPACE_EVENLY`. Default `TOP`. For COLUMN: arranges children vertically. For ROW: only `TOP`/`CENTER`/`BOTTOM` are meaningful and act as vertical alignment of children. |
| spacing               | Int?                    | COLUMN, ROW     | Gap between children in dp. Default `0`. Ignored when arrangement is `SPACE_*`. |
| onClick               | Action?                 | BUTTON, IMAGE   | Optional. See 4.4.                                                       |

#### Padding

```json
{ "start": 16, "top": 8, "end": 16, "bottom": 8 }
```

All four fields are `Int` in dp, all required when the object is present.

#### Size

```json
{ "mode": "FILL" }          // fill max width/height of parent
{ "mode": "WRAP" }          // wrap content
{ "mode": "FIXED", "dp": 200 }
```

`mode` is an enum `FILL | WRAP | FIXED`; `dp` is required only for `FIXED`.

### 4.4 Action (onClick)

Optional in the POC, but the shape is fixed now so the DTO does not change later:

```json
{ "type": "SHOW_TOAST", "payload": { "message": "Hello from server" } }
```

| type        | payload keys | Behaviour in the POC                                   |
|-------------|--------------|--------------------------------------------------------|
| SHOW_TOAST  | `message`    | Implemented. Shows an Android toast with the message.  |
| OPEN_URL    | `url`        | Implemented. Opens the URL in the system browser.      |
| NAVIGATE    | `screen`     | Parsed but not implemented (single screen in POC). Logs a message. |

Unknown action types are ignored and logged.

### 4.5 Example screen document (also used as seed data)

```json
{
  "_id": "3f0c1b2e-9a1d-4a4c-8f1e-1e2d3c4b5a60",
  "name": "home",
  "root": {
    "id": "b1e7e9c0-0000-4000-8000-000000000001",
    "type": "COLUMN",
    "containerColor": "#FFF5F5F5",
    "padding": { "start": 16, "top": 16, "end": 16, "bottom": 16 },
    "horizontalAlignment": "CENTER",
    "verticalArrangement": "TOP",
    "spacing": 12,
    "children": [
      {
        "id": "b1e7e9c0-0000-4000-8000-000000000002",
        "type": "ROW",
        "containerColor": "#FF5C738A",
        "padding": { "start": 12, "top": 12, "end": 12, "bottom": 12 },
        "horizontalAlignment": "START",
        "verticalArrangement": "CENTER",
        "spacing": 8,
        "children": [
          {
            "id": "b1e7e9c0-0000-4000-8000-000000000003",
            "type": "IMAGE",
            "imageContent": "https://picsum.photos/id/237/200/200",
            "contentDescription": "Avatar",
            "width": { "mode": "FIXED", "dp": 48 },
            "height": { "mode": "FIXED", "dp": 48 }
          },
          {
            "id": "b1e7e9c0-0000-4000-8000-000000000004",
            "type": "TEXT",
            "textContent": "Welcome to SSUI",
            "contentColor": "#FFFFFFFF"
          }
        ]
      },
      {
        "id": "b1e7e9c0-0000-4000-8000-000000000005",
        "type": "IMAGE",
        "imageContent": "https://picsum.photos/id/1015/800/400",
        "width": { "mode": "FILL" },
        "height": { "mode": "FIXED", "dp": 180 }
      },
      {
        "id": "b1e7e9c0-0000-4000-8000-000000000006",
        "type": "TEXT",
        "textContent": "This whole screen is described by a JSON document in MongoDB.",
        "contentColor": "#FF222222"
      },
      {
        "id": "b1e7e9c0-0000-4000-8000-000000000007",
        "type": "BUTTON",
        "textContent": "Tap me",
        "containerColor": "#FF5C738A",
        "contentColor": "#FFFFFFFF",
        "onClick": { "type": "SHOW_TOAST", "payload": { "message": "Hello from the server!" } }
      }
    ]
  }
}
```

## 5. Backend (`ssui-backend`)

### 5.1 Stack

| Item             | Choice                                                  |
|------------------|---------------------------------------------------------|
| Language         | Kotlin 2.x                                              |
| Framework        | Spring Boot 3.x (Spring Web MVC, Spring Data MongoDB)   |
| JVM              | Java 21                                                 |
| Build            | Gradle Kotlin DSL, version catalog (`libs.versions.toml`) |
| JSON             | Jackson (Spring default) with the Kotlin module          |
| Database         | MongoDB 7                                               |
| Containerization | Multi-stage `Dockerfile` (Gradle build stage + JRE 21 runtime stage) |
| Orchestration    | `docker-compose.yml` with services `backend` and `mongo` |
| Tests            | JUnit 5 unit tests for the entity<->DTO mapper and a `@WebMvcTest` for the controller with a mocked service |

Base package: `com.ssui.backend`.

### 5.2 Package structure

```
com.ssui.backend
├── SsuiBackendApplication.kt
├── api/            # controllers, DTOs (ScreenDto, UiElementDto, ActionDto, PaddingDto, SizeDto, enums), error handling
├── domain/         # ScreenService
├── persistence/    # ScreenEntity, UiElementEntity, ... + ScreenRepository (MongoRepository)
├── mapping/        # entity <-> DTO mappers
└── seed/           # DataSeeder (CommandLineRunner)
```

### 5.3 API

Base path: `/api/v1`. Content type: `application/json`.

| Method | Path                  | Description                                     | Responses |
|--------|-----------------------|-------------------------------------------------|-----------|
| GET    | `/screens/{name}`     | Returns the screen with the given `name`.       | `200` ScreenDto; `404` if not found |
| GET    | `/screens`            | Returns a list of `{ id, name }` for all screens. Handy for debugging. | `200` |
| PUT    | `/screens/{name}`     | Creates or fully replaces a screen. Body: ScreenDto (the `name` in the path wins). Used to edit UI without touching Mongo directly. | `200` ScreenDto; `400` on validation error |

Error body for `4xx`: `{ "status": 404, "message": "Screen 'home' not found" }`.

Validation on PUT (return `400`): `root.type` must be `COLUMN`; every `SIZE` with mode `FIXED` must have `dp > 0`; `IMAGE` must have `imageContent`; `TEXT` and `BUTTON` must have `textContent`.

### 5.4 Seeding

On startup a `CommandLineRunner` checks whether the `screens` collection contains a screen named `home`. If not, it loads `src/main/resources/seed/home.json` (the example from 4.5) and inserts it. Existing data is never overwritten, so edits made in Mongo survive restarts.

### 5.5 Docker

- `docker-compose.yml` at the repo root:
  - `mongo`: image `mongo:7`, port `27017:27017` exposed for inspection with Compass/mongosh, named volume `mongo-data`.
  - `backend`: built from the local `Dockerfile`, port `8080:8080`, depends on `mongo`, env var `SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/ssui`.
- Local run without Docker must also work: `./gradlew bootRun` with default URI `mongodb://localhost:27017/ssui`.
- Health check endpoint via Spring Actuator: `GET /actuator/health`.

## 6. Mobile app (`ssui-mobile`)

### 6.1 Stack

| Item          | Choice                                                                   |
|---------------|--------------------------------------------------------------------------|
| Language      | Kotlin 2.x                                                               |
| Min / target  | minSdk 26, targetSdk = compileSdk = latest stable                        |
| UI            | Jetpack Compose (Compose BOM, Material 3)                                |
| Networking    | Ktor Client (OkHttp engine) + `content-negotiation` + kotlinx.serialization JSON, `ignoreUnknownKeys = true`, unknown enum values fail parsing of that element only (see 6.4) |
| Images        | Coil 3 (`coil-compose` + `coil-network-okhttp`)                          |
| Async         | Kotlin Coroutines + Flow                                                 |
| DI            | Koin (lightweight, no annotation processing)                             |
| Architecture  | MVVM + MVI: single immutable `UiState`, single `onEvent(Event)` entry point in the ViewModel |
| Build         | Gradle Kotlin DSL, version catalog                                       |
| Tests         | Unit tests for the DTO->domain mapper and the ViewModel (with a fake repository) |

Application id / base package: `com.ssui.mobile`. App display name: `SSUI`.

### 6.2 Configuration

- Backend base URL comes from `BuildConfig.BASE_URL`, defined in `build.gradle.kts` as `"http://10.0.2.2:8080"` for the `debug` build type.
- Screen loaded at startup is hardcoded: `"home"`.
- `android:usesCleartextTraffic="true"` in the manifest (POC only).

### 6.3 Package structure

```
com.ssui.mobile
├── SsuiApp.kt                 # Application, Koin start
├── MainActivity.kt
├── di/                        # Koin modules
├── data/
│   ├── remote/                # Ktor client, ScreenApi, DTOs (mirror of backend DTOs)
│   ├── mapper/                # DTO -> domain
│   └── ScreenRepositoryImpl.kt
├── domain/                    # Screen, UiElement, Action, Padding, Size, enums; ScreenRepository interface
└── ui/
    ├── screen/                # ScreenViewModel, ScreenUiState, ScreenEvent, ScreenRoute (composable)
    ├── render/                # SsuiRenderer: recursive composable, one @Composable per element type
    └── theme/
```

### 6.4 MVI contract

```kotlin
sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Success(val screen: Screen) : ScreenUiState
    data class Error(val message: String) : ScreenUiState
}

sealed interface ScreenEvent {
    data object Load : ScreenEvent
    data object Retry : ScreenEvent
    data object Refresh : ScreenEvent          // refresh icon in the top app bar
    data class ElementClicked(val elementId: String, val action: Action) : ScreenEvent
}

sealed interface ScreenEffect {              // one-shot side effects, emitted via a Channel/SharedFlow
    data class ShowToast(val message: String) : ScreenEffect
    data class OpenUrl(val url: String) : ScreenEffect
}
```

- `Load` is sent once when the screen enters composition. `Retry` is sent from a button in the `Error` state.
- `Refresh` is sent from a refresh icon in a Material 3 top app bar (titled "SSUI") that is always visible. It re-fetches the screen from the backend and goes through `Loading` again, so edits made in Mongo or via `PUT` appear without relaunching the app. The icon is disabled while `Loading`.
- `ElementClicked` resolves the `Action` into a `ScreenEffect`; the composable collects effects and performs them (toast, intent).
- Rendering rules: a recursive `RenderElement(element)` composable switches on `type` and delegates to `SsuiColumn`, `SsuiRow`, `SsuiText`, `SsuiButton`, `SsuiImage`. Elements with an unknown type render nothing. An element whose JSON fails to parse is dropped and logged; the rest of the screen still renders.
- Mapping of layout fields to Compose: `padding` -> `Modifier.padding`, `width/height` -> `fillMaxWidth/Height`, `wrapContent*`, `width(dp)/height(dp)`; `horizontalAlignment` and `verticalArrangement` -> `Column`/`Row` parameters; `spacing` -> `Arrangement.spacedBy`; colors parsed from hex to `Color`, falling back to theme colors when `null`. IMAGE uses `ContentScale.Crop`.

## 7. Acceptance criteria (definition of done)

1. In `ssui-backend`, `docker compose up --build` starts `mongo` and `backend`. `GET http://localhost:8080/api/v1/screens/home` returns the seeded screen as JSON; `GET .../screens/unknown` returns `404`.
2. In `ssui-mobile`, the app builds from Android Studio, installs on an emulator (API 34+), and on launch shows a loading indicator, then renders the `home` screen: a grey column containing a blue-grey row (avatar image + white "Welcome to SSUI" text), a wide landscape image, a body text, and a "Tap me" button.
3. Tapping the button shows a toast with the message that came from the server.
4. Changing `textContent` of the button in Mongo (via Compass, mongosh or `PUT /api/v1/screens/home`) and tapping the refresh icon in the top bar (or relaunching the app) shows the new text without rebuilding the app.
5. With the backend stopped, the app shows an error state with a Retry button; starting the backend and tapping Retry renders the screen.
6. Backend and mobile unit tests pass (`./gradlew test` in both repos).

## 8. Out of scope for the POC

- Authentication, authorization, HTTPS.
- Multiple screens / navigation between server-driven screens (`NAVIGATE` is parsed only).
- Caching, offline mode, retry policies, pagination.
- Self-hosted images or an image upload API.
- Text styling (font size, weight, alignment), shapes, elevation, borders, animations.
- Additional components (text field, list, checkbox, spacer, card, etc.).
- Admin UI / editor for screens.
- CI pipelines, release signing, Play Store.
- iOS / KMP.

## 9. Possible next steps after the POC

- `NAVIGATE` action with a simple stack of server-driven screens.
- Versioning of screens and ETag-based caching in the app.
- More components and a text style object.
- Shared Kotlin module (KMP) for DTOs used by both backend and mobile.
