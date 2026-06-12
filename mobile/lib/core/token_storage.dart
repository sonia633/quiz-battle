import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Persists the JWT access/refresh pair in platform secure storage
/// (Keychain on iOS, EncryptedSharedPreferences on Android).
class TokenStorage {
  TokenStorage([FlutterSecureStorage? storage])
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  static const _accessKey = 'access_token';
  static const _refreshKey = 'refresh_token';

  Future<void> save({required String access, required String refresh}) async {
    await _storage.write(key: _accessKey, value: access);
    await _storage.write(key: _refreshKey, value: refresh);
  }

  Future<String?> get accessToken => _storage.read(key: _accessKey);

  Future<String?> get refreshToken => _storage.read(key: _refreshKey);

  Future<void> updateAccess(String access) =>
      _storage.write(key: _accessKey, value: access);

  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }

  Future<bool> get hasSession async => (await accessToken) != null;
}
