import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/game.dart';
import '../models/leaderboard.dart';
import '../models/quiz.dart';
import 'auth_controller.dart';
import 'providers.dart';

final categoriesProvider = FutureProvider<List<Category>>((ref) {
  return ref.watch(quizServiceProvider).categories();
});

/// Quizzes, optionally filtered by category id (null = all published).
final quizzesProvider =
    FutureProvider.family<List<QuizSummary>, int?>((ref, categoryId) {
  return ref.watch(quizServiceProvider).quizzes(categoryId: categoryId);
});

final quizDetailProvider =
    FutureProvider.family<QuizDetail, int>((ref, quizId) {
  return ref.watch(quizServiceProvider).quizForPlaying(quizId);
});

/// Leaderboard for a scope: global | weekly | monthly.
final leaderboardProvider =
    FutureProvider.family<List<RankingEntry>, String>((ref, scope) {
  return ref.watch(leaderboardServiceProvider).ranking(scope);
});

/// Room metadata by join code (used to tell the host apart from players).
final roomInfoProvider =
    FutureProvider.family<RoomInfo, String>((ref, code) {
  return ref.watch(quizServiceProvider).roomByCode(code);
});

/// Stats for the currently authenticated player (used by the dashboard).
final myStatsProvider = FutureProvider<PlayerStats?>((ref) async {
  final auth = ref.watch(authControllerProvider);
  final user = auth.user;
  if (user == null) return null;
  return ref.watch(leaderboardServiceProvider).playerStats(user.id);
});
