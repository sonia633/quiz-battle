import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../core/api_client.dart';
import '../core/token_storage.dart';
import '../services/auth_service.dart';
import '../services/leaderboard_service.dart';
import '../services/quiz_service.dart';
import 'auth_controller.dart';

/// Secure token storage.
final tokenStorageProvider = Provider<TokenStorage>((ref) => TokenStorage());

/// Configured Dio-backed API client. On session expiry it flips the auth
/// controller to logged-out.
final apiClientProvider = Provider<ApiClient>((ref) {
  final storage = ref.watch(tokenStorageProvider);
  return ApiClient(
    tokenStorage: storage,
    onSessionExpired: () => ref.read(authControllerProvider.notifier).onExpired(),
  );
});

final authServiceProvider =
    Provider<AuthService>((ref) => AuthService(ref.watch(apiClientProvider)));

final quizServiceProvider =
    Provider<QuizService>((ref) => QuizService(ref.watch(apiClientProvider)));

final leaderboardServiceProvider = Provider<LeaderboardService>(
    (ref) => LeaderboardService(ref.watch(apiClientProvider)));
