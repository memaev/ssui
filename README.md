# SSUI - Server Side UI (proof of concept)

A proof of concept for Server-Driven UI on Android: the screen layout lives as a JSON document in
MongoDB, a Kotlin/Spring Boot backend serves it over HTTP, and a Jetpack Compose app renders it.
Change the document on the server, tap refresh in the app, and the UI changes without a new build.

| Folder | What it is | Docs |
|---|---|---|
| [`main-idea.md`](main-idea.md) | The spec: goals, data model, API, stacks, acceptance criteria | |
| [`ssui-backend/`](ssui-backend) | Kotlin 2 + Spring Boot 3 + MongoDB 7, runs with `docker compose` | [README](ssui-backend/README.md) |
| [`ssui-mobile/`](ssui-mobile) | Android app: Kotlin 2, Jetpack Compose (Material 3), Ktor, Coil, Koin, MVI | [README](ssui-mobile/README.md) |

## Quick start

```bash
# 1. Backend + Mongo (host port 8080; override with BACKEND_PORT=8081 if 8080 is busy)
cd ssui-backend
docker compose up --build -d
curl -s http://localhost:8080/api/v1/screens/home | jq .

# 2. Android app on an emulator (reaches the host via 10.0.2.2:8080)
cd ../ssui-mobile
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.ssui.mobile/.MainActivity
```

## Editing the UI

Change the `home` screen document and tap the refresh icon in the app:

```bash
BASE=http://localhost:8080/api/v1
curl -s $BASE/screens/home \
  | jq '.root.children[3].textContent = "Hello from the server"' \
  | curl -s -X PUT -H 'Content-Type: application/json' -d @- $BASE/screens/home
```

See the mobile README for mongosh and Compass alternatives, and the spec section 4 for every field.

## Tests

```bash
(cd ssui-backend && ./gradlew test)   # 32 tests
(cd ssui-mobile  && ./gradlew test)   # 24 tests
```
