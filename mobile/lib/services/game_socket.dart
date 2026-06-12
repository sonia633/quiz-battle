import 'dart:async';
import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

import '../core/config.dart';
import '../models/game.dart';

/// STOMP-over-SockJS client for live battles. Mirrors the destinations exposed
/// by the Java `quiz-service`:
///   * subscribe  `/topic/rooms/{code}`
///   * publish    `/app/rooms/{code}/join` | `/answer` | `/leave`
class GameSocket {
  GameSocket();

  StompClient? _client;
  final _events = StreamController<GameEvent>.broadcast();
  String? _code;

  /// Stream of decoded game events for the joined room.
  Stream<GameEvent> get events => _events.stream;

  bool get isConnected => _client?.connected ?? false;

  /// SockJS expects an http(s) URL; convert the configured ws(s) URL.
  static String get _sockJsUrl => AppConfig.wsUrl
      .replaceFirstMapped(RegExp(r'^ws(s?)://'), (m) => 'http${m[1]}://');

  Future<void> connect({
    required String roomCode,
    required int userId,
    required String username,
  }) {
    _code = roomCode;
    final completer = Completer<void>();

    _client = StompClient(
      config: StompConfig.sockJS(
        url: _sockJsUrl,
        onConnect: (StompFrame frame) {
          _client!.subscribe(
            destination: '/topic/rooms/$roomCode',
            callback: (StompFrame f) {
              if (f.body == null) return;
              final json = jsonDecode(f.body!) as Map<String, dynamic>;
              _events.add(GameEvent.fromJson(json));
            },
          );
          _send('/app/rooms/$roomCode/join',
              {'userId': userId, 'username': username});
          if (!completer.isCompleted) completer.complete();
        },
        onWebSocketError: (dynamic error) {
          if (!completer.isCompleted) completer.completeError(error);
          _events.addError(error);
        },
      ),
    );

    _client!.activate();
    return completer.future;
  }

  void submitAnswer({
    required int userId,
    required int questionId,
    required int answerId,
    required int elapsedMillis,
  }) {
    if (_code == null) return;
    _send('/app/rooms/$_code/answer', {
      'userId': userId,
      'answer': {
        'questionId': questionId,
        'answerId': answerId,
        'elapsedMillis': elapsedMillis,
      },
    });
  }

  void leave(int userId, String username) {
    if (_code == null) return;
    _send('/app/rooms/$_code/leave', {'userId': userId, 'username': username});
  }

  void _send(String destination, Map<String, dynamic> body) {
    _client?.send(destination: destination, body: jsonEncode(body));
  }

  Future<void> dispose() async {
    _client?.deactivate();
    await _events.close();
  }
}
