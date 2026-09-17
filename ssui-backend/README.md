# ssui-backend

Backend of the **SSUI (Server Side UI)** proof of concept: a stateless Kotlin / Spring Boot REST service that stores
screen definitions (a tree of UI elements) in MongoDB and serves them as JSON to the Android app
(`../ssui-mobile`). The full specification lives in `../main-idea.md`; this repo implements sections 4, 5 and 7.

## Stack

| Item      | Choice                                                                   |
|-----------|--------------------------------------------------------------------------|
| Language  | Kotlin 2.2, Java 21                                                      |
| Framework | Spring Boot 3.5 (Web MVC, Spring Data MongoDB, Actuator), Jackson Kotlin |
| Build     | Gradle 8.14 Kotlin DSL, version catalog in `gradle/libs.versions.toml`   |
| Database  | MongoDB 7                                                                |
| Runtime   | Multi-stage `Dockerfile` (Gradle JDK 21 build, Temurin 21 JRE runtime)   |

```
com.ssui.backend
├── SsuiBackendApplication.kt
├── api/            ScreenController, DTOs (ScreenDto, UiElementDto, ...), enums, ScreenValidator, ApiExceptionHandler
├── domain/         ScreenService
├── persistence/    ScreenEntity + embedded entities, ScreenRepository (MongoRepository), MongoConfig
├── mapping/        ScreenMapper (entity <-> DTO)
└── seed/           DataSeeder (CommandLineRunner), SeedScreenReader
```

## Run with Docker Compose (recommended)

Requires Docker with Compose v2.

```bash
docker compose up --build -d          # builds the image, starts mongo + backend
docker compose logs -f backend        # follow the application log
docker compose down                   # stop (keeps the mongo-data volume)
docker compose down -v                # stop and wipe the database
```

Services:

| Service   | Image / build | Ports                       | Notes                                               |
|-----------|---------------|-----------------------------|-----------------------------------------------------|
| `mongo`   | `mongo:7`     | `27017:27017`               | Named volume `mongo-data`; healthcheck via `mongosh` |
| `backend` | `./Dockerfile`| `${BACKEND_PORT:-8080}:8080` | Waits for `mongo` to be healthy; `SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/ssui` |

### Changing the host port

The backend listens on 8080 inside the container. The **host** port defaults to 8080 and can be overridden with
`BACKEND_PORT` when something else already occupies 8080 on your machine:

```bash
BACKEND_PORT=8081 docker compose up --build -d
curl -s http://localhost:8081/actuator/health
```

Note: the Android app talks to `http://10.0.2.2:8080` (spec 6.2), so use the default port when testing with the
emulator, or adjust `BuildConfig.BASE_URL` in the mobile project.

Health: `curl -s http://localhost:8080/actuator/health` returns `{"status":"UP"}` once Mongo is reachable.

## Run locally without Docker

You need Java 21 and a MongoDB reachable at `mongodb://localhost:27017/ssui` (for example `docker compose up -d mongo`).

```bash
./gradlew bootRun
# or with a different database
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/ssui-dev ./gradlew bootRun
```

Configuration (`src/main/resources/application.yml`):

| Property / env var          | Default                           |
|-----------------------------|-----------------------------------|
| `SPRING_DATA_MONGODB_URI`   | `mongodb://localhost:27017/ssui`  |
| `server.port`               | `8080`                            |
| Actuator                    | `GET /actuator/health` (+ `/liveness`, `/readiness`) |

## Build and test

```bash
./gradlew build      # compiles, runs all tests, produces build/libs/ssui-backend-0.1.0.jar
./gradlew test       # tests only (no MongoDB needed: mapper/validator/service unit tests + @WebMvcTest slice)
```

## Seeding

On startup `DataSeeder` inserts `src/main/resources/seed/home.json` (the exact example document from spec 4.5)
**only if** no screen named `home` exists. Existing data is never overwritten, so edits made via Compass, mongosh or
`PUT` survive restarts. To reset to the seed: `docker compose down -v && docker compose up -d`.

Stored documents look exactly like the seed (`_id` is the UUID string, no `_class` discriminator is written).

## API

Base path `/api/v1`, content type `application/json`.

| Method | Path              | Description                                                     | Responses                          |
|--------|-------------------|-----------------------------------------------------------------|------------------------------------|
| GET    | `/screens/{name}` | Full screen tree                                                | `200` ScreenDto, `404`             |
| GET    | `/screens`        | `[{ id, name }, ...]` of all screens (debugging)                | `200`                              |
| PUT    | `/screens/{name}` | Create or fully replace; the `name` in the path wins over body  | `200` ScreenDto, `400` validation  |

Error body (all non-2xx): `{ "status": 404, "message": "Screen 'home' not found" }`.

Validation on PUT (`400`): `root.type` must be `COLUMN`; every size with `mode: FIXED` needs `dp > 0`;
`IMAGE` needs `imageContent`; `TEXT` and `BUTTON` need `textContent`. Malformed JSON, missing required fields and
unknown enum values also yield `400` in the same body shape. Unknown JSON properties are ignored.

On PUT the id is resolved as: keep the existing screen's id when replacing; otherwise use the body's `id` if present,
else generate a UUID.

### Example requests

```bash
BASE=http://localhost:8080/api/v1

# The seeded home screen
curl -s $BASE/screens/home | jq .

# List all screens
curl -s $BASE/screens | jq .

# 404
curl -s -i $BASE/screens/unknown
# -> HTTP/1.1 404  {"status":404,"message":"Screen 'unknown' not found"}

# Change the button label without rebuilding the app (root.children[3] is the BUTTON);
# then tap the refresh icon in the mobile app to see it
curl -s $BASE/screens/home \
  | jq '.root.children[3].textContent = "Tap me again"' \
  | curl -s -X PUT -H 'Content-Type: application/json' -d @- $BASE/screens/home | jq '.root.children[3].textContent'

# Validation error: root must be a COLUMN
curl -s -X PUT -H 'Content-Type: application/json' $BASE/screens/home \
  -d '{"root":{"id":"r","type":"ROW","children":[]}}'
# -> {"status":400,"message":"Invalid screen: root.type must be COLUMN but was ROW"}

# Restore the spec example (the seed file is a Mongo document with _id; the API accepts it as-is,
# the id is kept because the screen already exists)
curl -s -X PUT -H 'Content-Type: application/json' $BASE/screens/home \
  -d @src/main/resources/seed/home.json | jq '.root.children[3].textContent'

# Create a second screen
curl -s -X PUT -H 'Content-Type: application/json' $BASE/screens/about \
  -d '{"root":{"id":"a1","type":"COLUMN","children":[{"id":"a2","type":"TEXT","textContent":"About"}]}}' | jq .
```

Inspect the database directly:

```bash
docker compose exec mongo mongosh ssui --quiet --eval 'db.screens.find().pretty()'
```

## Data model (summary)

See spec sections 4.2 - 4.5. One universal `UiElement` with a `type` discriminator
(`COLUMN | ROW | TEXT | BUTTON | IMAGE`), nullable type-specific fields, `children` always present as a list
(empty for leaves), colors as ARGB hex strings (`#FF5C738A`), all sizes in dp, `Size { mode: FILL|WRAP|FIXED, dp? }`,
`Padding { start, top, end, bottom }`, `onClick: Action { type: SHOW_TOAST|OPEN_URL|NAVIGATE, payload }`.
