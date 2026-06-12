import '../core/api_client.dart';
import '../models/user.dart';

/// Talks to the auth + user endpoints on the quiz-service (via the gateway).
class AuthService {
  AuthService(this._api);

  final ApiClient _api;

  Future<AuthTokens> register({
    required String username,
    required String email,
    required String password,
    String? displayName,
  }) async {
    final res = await _api.dio.post('/api/auth/register', data: {
      'username': username,
      'email': email,
      'password': password,
      'displayName': displayName,
    });
    return AuthTokens.fromJson(res.data as Map<String, dynamic>);
  }

  Future<AuthTokens> login({
    required String usernameOrEmail,
    required String password,
  }) async {
    final res = await _api.dio.post('/api/auth/login', data: {
      'usernameOrEmail': usernameOrEmail,
      'password': password,
    });
    return AuthTokens.fromJson(res.data as Map<String, dynamic>);
  }

  Future<UserProfile> me() async {
    final res = await _api.dio.get('/api/users/me');
    return UserProfile.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> logout() => _api.dio.post('/api/auth/logout');
}
