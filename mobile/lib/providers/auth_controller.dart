import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/user.dart';
import 'providers.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

class AuthState {
  const AuthState({required this.status, this.user, this.error});

  final AuthStatus status;
  final UserProfile? user;
  final String? error;

  AuthState copyWith({AuthStatus? status, UserProfile? user, String? error}) =>
      AuthState(
        status: status ?? this.status,
        user: user ?? this.user,
        error: error,
      );

  static const unknown = AuthState(status: AuthStatus.unknown);
}

/// Owns the authentication lifecycle: bootstrap from stored tokens, login,
/// register and logout.
class AuthController extends StateNotifier<AuthState> {
  AuthController(this._ref) : super(AuthState.unknown) {
    _bootstrap();
  }

  final Ref _ref;

  Future<void> _bootstrap() async {
    final storage = _ref.read(tokenStorageProvider);
    if (!await storage.hasSession) {
      state = const AuthState(status: AuthStatus.unauthenticated);
      return;
    }
    try {
      final user = await _ref.read(authServiceProvider).me();
      state = AuthState(status: AuthStatus.authenticated, user: user);
    } catch (_) {
      await storage.clear();
      state = const AuthState(status: AuthStatus.unauthenticated);
    }
  }

  Future<bool> login(String usernameOrEmail, String password) async {
    return _run(() => _ref
        .read(authServiceProvider)
        .login(usernameOrEmail: usernameOrEmail, password: password));
  }

  Future<bool> register({
    required String username,
    required String email,
    required String password,
    String? displayName,
  }) async {
    return _run(() => _ref.read(authServiceProvider).register(
          username: username,
          email: email,
          password: password,
          displayName: displayName,
        ));
  }

  Future<bool> _run(Future<AuthTokens> Function() action) async {
    state = state.copyWith(status: AuthStatus.unknown, error: null);
    try {
      final tokens = await action();
      await _ref
          .read(tokenStorageProvider)
          .save(access: tokens.accessToken, refresh: tokens.refreshToken);
      state = AuthState(status: AuthStatus.authenticated, user: tokens.user);
      return true;
    } catch (e) {
      state = AuthState(
        status: AuthStatus.unauthenticated,
        error: _humanize(e),
      );
      return false;
    }
  }

  Future<void> logout() async {
    try {
      await _ref.read(authServiceProvider).logout();
    } catch (_) {
      // Best effort — clear locally regardless.
    }
    await _ref.read(tokenStorageProvider).clear();
    state = const AuthState(status: AuthStatus.unauthenticated);
  }

  /// Invoked by the API client when a refresh fails.
  void onExpired() {
    state = const AuthState(
      status: AuthStatus.unauthenticated,
      error: 'Your session expired. Please sign in again.',
    );
  }

  String _humanize(Object e) {
    final text = e.toString();
    if (text.contains('401')) return 'Invalid credentials.';
    if (text.contains('409')) return 'Username or email already in use.';
    if (text.contains('SocketException') || text.contains('connection')) {
      return 'Cannot reach the server. Is the backend running?';
    }
    return 'Something went wrong. Please try again.';
  }
}

final authControllerProvider =
    StateNotifierProvider<AuthController, AuthState>((ref) => AuthController(ref));
