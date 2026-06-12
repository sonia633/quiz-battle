import 'package:dio/dio.dart';

import 'config.dart';
import 'token_storage.dart';

/// Thin wrapper around [Dio] that injects the bearer token and transparently
/// refreshes it on a 401, retrying the original request once. If refresh fails
/// the session is cleared and [onSessionExpired] is invoked.
class ApiClient {
  ApiClient({required TokenStorage tokenStorage, this.onSessionExpired})
      : _tokens = tokenStorage,
        dio = Dio(
          BaseOptions(
            baseUrl: AppConfig.apiBaseUrl,
            connectTimeout: AppConfig.connectTimeout,
            receiveTimeout: AppConfig.receiveTimeout,
            contentType: 'application/json',
          ),
        ) {
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: _onRequest,
        onError: _onError,
      ),
    );
  }

  final Dio dio;
  final TokenStorage _tokens;
  final void Function()? onSessionExpired;

  bool _refreshing = false;

  Future<void> _onRequest(
      RequestOptions options, RequestInterceptorHandler handler) async {
    // The login/register/refresh endpoints must not carry a (possibly stale) token.
    if (!options.path.contains('/api/auth/')) {
      final token = await _tokens.accessToken;
      if (token != null) {
        options.headers['Authorization'] = 'Bearer $token';
      }
    }
    handler.next(options);
  }

  Future<void> _onError(
      DioException error, ErrorInterceptorHandler handler) async {
    final isAuthCall = error.requestOptions.path.contains('/api/auth/');
    if (error.response?.statusCode != 401 || isAuthCall || _refreshing) {
      return handler.next(error);
    }

    final refreshed = await _tryRefresh();
    if (!refreshed) {
      await _tokens.clear();
      onSessionExpired?.call();
      return handler.next(error);
    }

    // Retry the original request with the new token.
    try {
      final token = await _tokens.accessToken;
      final opts = error.requestOptions;
      opts.headers['Authorization'] = 'Bearer $token';
      final response = await dio.fetch(opts);
      return handler.resolve(response);
    } on DioException catch (e) {
      return handler.next(e);
    }
  }

  Future<bool> _tryRefresh() async {
    final refresh = await _tokens.refreshToken;
    if (refresh == null) return false;

    _refreshing = true;
    try {
      final response = await dio.post(
        '/api/auth/refresh',
        data: {'refreshToken': refresh},
      );
      final data = response.data as Map<String, dynamic>;
      await _tokens.save(
        access: data['accessToken'] as String,
        refresh: data['refreshToken'] as String,
      );
      return true;
    } on DioException {
      return false;
    } finally {
      _refreshing = false;
    }
  }
}
