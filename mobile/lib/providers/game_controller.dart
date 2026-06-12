import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/game.dart';
import '../models/quiz.dart';
import '../services/game_socket.dart';
import 'auth_controller.dart';

enum GamePhase { lobby, question, reveal, finished }

class GameState {
  const GameState({
    this.phase = GamePhase.lobby,
    this.currentQuestion,
    this.scoreboard = const [],
    this.totalQuestions = 0,
    this.answered = false,
    this.lastCorrect,
    this.lastPoints,
    this.myScore = 0,
    this.error,
  });

  final GamePhase phase;
  final Question? currentQuestion;
  final List<PlayerScore> scoreboard;
  final int totalQuestions;
  final bool answered;
  final bool? lastCorrect;
  final int? lastPoints;
  final int myScore;
  final String? error;

  GameState copyWith({
    GamePhase? phase,
    Question? currentQuestion,
    List<PlayerScore>? scoreboard,
    int? totalQuestions,
    bool? answered,
    bool? lastCorrect,
    int? lastPoints,
    int? myScore,
    String? error,
  }) =>
      GameState(
        phase: phase ?? this.phase,
        currentQuestion: currentQuestion ?? this.currentQuestion,
        scoreboard: scoreboard ?? this.scoreboard,
        totalQuestions: totalQuestions ?? this.totalQuestions,
        answered: answered ?? this.answered,
        lastCorrect: lastCorrect ?? this.lastCorrect,
        lastPoints: lastPoints ?? this.lastPoints,
        myScore: myScore ?? this.myScore,
        error: error ?? this.error,
      );
}

/// Drives one multiplayer battle: connects the socket, maps incoming events to
/// [GameState], and forwards player answers.
class GameController extends StateNotifier<GameState> {
  GameController(this._ref, this.roomCode) : super(const GameState()) {
    _connect();
  }

  final Ref _ref;
  final String roomCode;
  final GameSocket _socket = GameSocket();

  int _userId = 0;
  String _username = 'player';
  DateTime? _questionShownAt;

  Future<void> _connect() async {
    final user = _ref.read(authControllerProvider).user;
    if (user == null) {
      state = state.copyWith(error: 'Not authenticated');
      return;
    }
    _userId = user.id;
    _username = user.displayName;
    _socket.events.listen(_onEvent, onError: (e) {
      state = state.copyWith(error: e.toString());
    });
    try {
      await _socket.connect(
        roomCode: roomCode,
        userId: user.id,
        username: user.displayName,
      );
    } catch (e) {
      state = state.copyWith(error: 'Could not connect to the room.');
    }
  }

  void _onEvent(GameEvent event) {
    switch (event.type) {
      case 'GAME_STARTED':
        final payload = event.payload as Map<String, dynamic>;
        state = state.copyWith(
          phase: GamePhase.lobby,
          totalQuestions: (payload['totalQuestions'] as num?)?.toInt() ?? 0,
        );
        break;
      case 'NEXT_QUESTION':
        _questionShownAt = DateTime.now();
        // Build a fresh round state so the previous answer's reveal is cleared.
        state = GameState(
          phase: GamePhase.question,
          currentQuestion:
              Question.fromJson(event.payload as Map<String, dynamic>),
          scoreboard: state.scoreboard,
          totalQuestions: state.totalQuestions,
          myScore: state.myScore,
          answered: false,
        );
        break;
      case 'ANSWER_RESULT':
        final p = event.payload as Map<String, dynamic>;
        if ((p['userId'] as num?)?.toInt() == _userId) {
          state = state.copyWith(
            phase: GamePhase.reveal,
            lastCorrect: p['correct'] as bool?,
            lastPoints: (p['pointsAwarded'] as num?)?.toInt(),
            myScore: (p['totalScore'] as num?)?.toInt() ?? state.myScore,
          );
        }
        break;
      case 'SCOREBOARD':
        state = state.copyWith(scoreboard: _parseScoreboard(event.payload));
        break;
      case 'GAME_FINISHED':
        state = state.copyWith(
          phase: GamePhase.finished,
          scoreboard: _parseScoreboard(event.payload),
        );
        break;
      default:
        break; // PLAYER_JOINED / PLAYER_LEFT handled via SCOREBOARD refresh
    }
  }

  List<PlayerScore> _parseScoreboard(dynamic payload) {
    final map = payload as Map<String, dynamic>;
    final players = (map['players'] as List?) ?? const [];
    return players
        .map((e) => PlayerScore.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  void answer(int answerId) {
    final q = state.currentQuestion;
    if (q == null || state.answered) return;
    final elapsed =
        DateTime.now().difference(_questionShownAt ?? DateTime.now()).inMilliseconds;
    _socket.submitAnswer(
      userId: _userId,
      questionId: q.id,
      answerId: answerId,
      elapsedMillis: elapsed,
    );
    state = state.copyWith(answered: true);
  }

  @override
  void dispose() {
    // Use the identity captured at connect time — reading providers during
    // disposal of an autoDispose notifier is unsafe.
    if (_userId != 0) _socket.leave(_userId, _username);
    _socket.dispose();
    super.dispose();
  }
}

final gameControllerProvider = StateNotifierProvider.autoDispose
    .family<GameController, GameState, String>(
  (ref, roomCode) => GameController(ref, roomCode),
);
