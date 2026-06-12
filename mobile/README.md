# Quiz Battle — Flutter Client

Cross-platform mobile client (Flutter + Dart) using **Riverpod** for state,
**go_router** for navigation, **Dio** for HTTP (with automatic JWT refresh), and
**STOMP over WebSocket** for live multiplayer battles.

## Features

- **Auth** — login / register against the gateway, secure token storage, silent
  access-token refresh, auth-aware routing.
- **Discover** — category grid → quiz list (practice or host a battle).
- **Practice** — solo run with progress + explanations (graded when the backend
  exposes correctness).
- **Multiplayer battle** — create/join a room by code, live questions, locked
  answers, real-time scoreboard and final podium over WebSocket.
- **Ranking** — global / weekly / monthly leaderboards.
- **Profile dashboard** — games, wins, accuracy, XP, level, accuracy gauge
  (fl_chart).

## Project layout

```
lib/
  core/        config, Dio client (+JWT refresh interceptor), secure token storage
  models/      plain data classes with fromJson
  services/    auth / quiz / leaderboard REST + STOMP game socket
  providers/   Riverpod providers, auth controller, live game controller
  features/    auth · home · quiz · room · leaderboard · profile screens
  app.dart     router + theme;  main.dart  entrypoint
```

## Run

```bash
flutter pub get

# The android/ios/web platform folders are not committed. Generate them once:
flutter create .

# Point the app at your backend (Android emulator reaches the host via 10.0.2.2):
flutter run \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=WS_URL=ws://10.0.2.2:8080/ws
```

Defaults target `http://localhost:8080` (the API gateway).

## Test

```bash
flutter test                                   # unit + widget tests
flutter test integration_test/app_test.dart    # integration (needs a device)
```

- `test/models_test.dart` — JSON parsing for every model.
- `test/login_screen_test.dart` — widget test of the sign-in form + validation.
- `integration_test/app_test.dart` — cold-start smoke test.
