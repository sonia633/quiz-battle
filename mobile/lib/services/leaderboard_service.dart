import '../core/api_client.dart';
import '../models/leaderboard.dart';

class LeaderboardService {
  LeaderboardService(this._api);

  final ApiClient _api;

  Future<List<RankingEntry>> ranking(String scope, {int limit = 50}) async {
    // scope: global | weekly | monthly
    final res = await _api.dio.get(
      '/api/leaderboard/$scope',
      queryParameters: {'limit': limit},
    );
    final entries = (res.data as Map<String, dynamic>)['entries'] as List? ?? [];
    return entries
        .map((e) => RankingEntry.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<PlayerStats> playerStats(int userId) async {
    final res = await _api.dio.get('/api/stats/players/$userId');
    return PlayerStats.fromJson(res.data as Map<String, dynamic>);
  }
}
