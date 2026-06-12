/// Centralised runtime configuration.
///
/// Override at build time, e.g.:
/// `flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080`
/// (use `10.0.2.2` for the Android emulator to reach the host machine).
class AppConfig {
  AppConfig._();

  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080',
  );

  /// SockJS/STOMP endpoint exposed by the quiz-service through the gateway.
  static const String wsUrl = String.fromEnvironment(
    'WS_URL',
    defaultValue: 'ws://localhost:8080/ws',
  );

  static const Duration connectTimeout = Duration(seconds: 10);
  static const Duration receiveTimeout = Duration(seconds: 15);
}
