import 'package:flutter_test/flutter_test.dart';
import 'package:quiz_battle/models/game.dart';
import 'package:quiz_battle/models/leaderboard.dart';
import 'package:quiz_battle/models/quiz.dart';
import 'package:quiz_battle/models/user.dart';

void main() {
  group('UserProfile', () {
    test('parses roles and derives admin/moderator flags', () {
      final user = UserProfile.fromJson({
        'id': 1,
        'username': 'alice',
        'email': 'a@b.c',
        'displayName': 'Alice',
        'xp': 1200,
        'roles': ['PLAYER', 'MODERATOR'],
      });
      expect(user.id, 1);
      expect(user.displayName, 'Alice');
      expect(user.isModerator, isTrue);
      expect(user.isAdmin, isFalse);
    });

    test('falls back displayName to username', () {
      final user = UserProfile.fromJson({
        'id': 2,
        'username': 'bob',
        'email': 'bob@b.c',
        'roles': <String>[],
      });
      expect(user.displayName, 'bob');
      expect(user.xp, 0);
    });
  });

  test('AuthTokens parses nested user', () {
    final tokens = AuthTokens.fromJson({
      'accessToken': 'acc',
      'refreshToken': 'ref',
      'tokenType': 'Bearer',
      'user': {'id': 5, 'username': 'eve', 'email': 'e@e.e', 'roles': ['PLAYER']},
    });
    expect(tokens.accessToken, 'acc');
    expect(tokens.user.username, 'eve');
  });

  test('Question keeps hidden correctness as null during play', () {
    final q = Question.fromJson({
      'id': 10,
      'text': '2+2?',
      'type': 'MULTIPLE_CHOICE',
      'points': 100,
      'answers': [
        {'id': 1, 'text': '4', 'correct': null},
        {'id': 2, 'text': '5', 'correct': null},
      ],
    });
    expect(q.answers, hasLength(2));
    expect(q.answers.first.correct, isNull);
  });

  test('RankingEntry parses ints defensively', () {
    final e = RankingEntry.fromJson(
        {'rank': 1, 'userId': 7, 'xp': 999, 'score': 4200, 'level': 5});
    expect(e.rank, 1);
    expect(e.level, 5);
  });

  test('GameEvent carries type and payload', () {
    final ev = GameEvent.fromJson({
      'type': 'NEXT_QUESTION',
      'payload': {'id': 1, 'text': 'Q'},
    });
    expect(ev.type, 'NEXT_QUESTION');
    expect((ev.payload as Map)['text'], 'Q');
  });

  test('Scoreboard player parses', () {
    final p = PlayerScore.fromJson(
        {'userId': 3, 'username': 'cara', 'score': 250, 'rank': 2});
    expect(p.username, 'cara');
    expect(p.score, 250);
  });
}
