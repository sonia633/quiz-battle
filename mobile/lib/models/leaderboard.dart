class RankingEntry {
  const RankingEntry({
    required this.rank,
    required this.userId,
    required this.xp,
    required this.score,
    required this.level,
  });

  final int rank;
  final int userId;
  final int xp;
  final int score;
  final int level;

  factory RankingEntry.fromJson(Map<String, dynamic> json) => RankingEntry(
        rank: (json['rank'] as num).toInt(),
        userId: (json['userId'] as num).toInt(),
        xp: (json['xp'] as num?)?.toInt() ?? 0,
        score: (json['score'] as num?)?.toInt() ?? 0,
        level: (json['level'] as num?)?.toInt() ?? 1,
      );
}

class PlayerStats {
  const PlayerStats({
    required this.userId,
    required this.gamesPlayed,
    required this.wins,
    required this.totalScore,
    required this.accuracy,
    required this.xp,
    required this.level,
  });

  final int userId;
  final int gamesPlayed;
  final int wins;
  final int totalScore;
  final double accuracy;
  final int xp;
  final int level;

  factory PlayerStats.fromJson(Map<String, dynamic> json) => PlayerStats(
        userId: (json['userId'] as num).toInt(),
        gamesPlayed: (json['gamesPlayed'] as num?)?.toInt() ?? 0,
        wins: (json['wins'] as num?)?.toInt() ?? 0,
        totalScore: (json['totalScore'] as num?)?.toInt() ?? 0,
        accuracy: (json['accuracy'] as num?)?.toDouble() ?? 0.0,
        xp: (json['xp'] as num?)?.toInt() ?? 0,
        level: (json['level'] as num?)?.toInt() ?? 1,
      );
}
