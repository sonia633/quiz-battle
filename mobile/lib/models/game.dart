/// Models for multiplayer rooms and the live game event stream.

class RoomInfo {
  const RoomInfo({
    required this.id,
    required this.code,
    required this.quizId,
    required this.quizTitle,
    required this.hostId,
    required this.status,
    required this.maxPlayers,
  });

  final int id;
  final String code;
  final int quizId;
  final String quizTitle;
  final int hostId;
  final String status;
  final int maxPlayers;

  factory RoomInfo.fromJson(Map<String, dynamic> json) => RoomInfo(
        id: json['id'] as int,
        code: json['code'] as String,
        quizId: json['quizId'] as int,
        quizTitle: json['quizTitle'] as String? ?? '',
        hostId: json['hostId'] as int,
        status: json['status'] as String,
        maxPlayers: (json['maxPlayers'] as num?)?.toInt() ?? 20,
      );
}

class PlayerScore {
  const PlayerScore({
    required this.userId,
    required this.username,
    required this.score,
    required this.rank,
  });

  final int userId;
  final String username;
  final int score;
  final int rank;

  factory PlayerScore.fromJson(Map<String, dynamic> json) => PlayerScore(
        userId: json['userId'] as int,
        username: json['username'] as String? ?? 'player',
        score: (json['score'] as num?)?.toInt() ?? 0,
        rank: (json['rank'] as num?)?.toInt() ?? 0,
      );
}

/// A decoded `/topic/rooms/{code}` event. The Java side sends
/// `{ "type": "...", "payload": {...} }`.
class GameEvent {
  const GameEvent({required this.type, required this.payload});

  final String type;
  final dynamic payload;

  factory GameEvent.fromJson(Map<String, dynamic> json) => GameEvent(
        type: json['type'] as String,
        payload: json['payload'],
      );
}
