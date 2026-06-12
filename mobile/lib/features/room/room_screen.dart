import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../models/game.dart';
import '../../models/quiz.dart';
import '../../providers/auth_controller.dart';
import '../../providers/data_providers.dart';
import '../../providers/game_controller.dart';
import '../../providers/providers.dart';

class RoomScreen extends ConsumerWidget {
  const RoomScreen({super.key, required this.roomCode});

  final String roomCode;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final game = ref.watch(gameControllerProvider(roomCode));
    final roomInfo = ref.watch(roomInfoProvider(roomCode));
    final myId = ref.watch(authControllerProvider).user?.id;
    final isHost = roomInfo.maybeWhen(
      data: (r) => r.hostId == myId,
      orElse: () => false,
    );

    return Scaffold(
      appBar: AppBar(title: Text('Room $roomCode')),
      body: SafeArea(
        child: Column(
          children: [
            if (game.error != null)
              MaterialBanner(
                content: Text(game.error!),
                actions: [
                  TextButton(onPressed: () => context.pop(), child: const Text('Leave')),
                ],
              ),
            Expanded(child: _phaseBody(context, ref, game, isHost)),
          ],
        ),
      ),
    );
  }

  Widget _phaseBody(BuildContext context, WidgetRef ref, GameState game, bool isHost) {
    switch (game.phase) {
      case GamePhase.lobby:
        return _Lobby(roomCode: roomCode, players: game.scoreboard, isHost: isHost);
      case GamePhase.question:
        return _QuestionView(
          question: game.currentQuestion,
          answered: game.answered,
          onAnswer: (id) =>
              ref.read(gameControllerProvider(roomCode).notifier).answer(id),
        );
      case GamePhase.reveal:
        return _RevealView(game: game);
      case GamePhase.finished:
        return _FinishedView(players: game.scoreboard);
    }
  }
}

class _Lobby extends ConsumerWidget {
  const _Lobby({required this.roomCode, required this.players, required this.isHost});

  final String roomCode;
  final List<PlayerScore> players;
  final bool isHost;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          Text('Share this code', style: theme.textTheme.bodyMedium),
          const SizedBox(height: 8),
          Text(roomCode,
              style: theme.textTheme.displaySmall
                  ?.copyWith(fontWeight: FontWeight.bold, letterSpacing: 6)),
          const SizedBox(height: 24),
          Text('Players (${players.length})', style: theme.textTheme.titleMedium),
          const SizedBox(height: 8),
          Expanded(
            child: players.isEmpty
                ? const Center(child: Text('Waiting for players to join…'))
                : ListView(
                    children: [
                      for (final p in players)
                        ListTile(
                          leading: const Icon(Icons.person),
                          title: Text(p.username),
                        ),
                    ],
                  ),
          ),
          if (isHost)
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                icon: const Icon(Icons.play_arrow),
                label: const Text('Start battle'),
                onPressed: () async {
                  final messenger = ScaffoldMessenger.of(context);
                  try {
                    await ref.read(quizServiceProvider).startRoom(roomCode);
                  } catch (_) {
                    messenger.showSnackBar(
                      const SnackBar(content: Text('Could not start the battle.')),
                    );
                  }
                },
              ),
            )
          else
            const Text('Waiting for the host to start…'),
        ],
      ),
    );
  }
}

class _QuestionView extends StatelessWidget {
  const _QuestionView({
    required this.question,
    required this.answered,
    required this.onAnswer,
  });

  final Question? question;
  final bool answered;
  final void Function(int answerId) onAnswer;

  @override
  Widget build(BuildContext context) {
    final q = question;
    if (q == null) {
      return const Center(child: CircularProgressIndicator());
    }
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(q.text,
              style: Theme.of(context)
                  .textTheme
                  .headlineSmall
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 24),
          Expanded(
            child: GridView.count(
              crossAxisCount: 2,
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.6,
              children: [
                for (final a in q.answers)
                  _AnswerButton(
                    text: a.text,
                    enabled: !answered,
                    onTap: () => onAnswer(a.id),
                  ),
              ],
            ),
          ),
          if (answered)
            const Padding(
              padding: EdgeInsets.only(top: 8),
              child: Text('Answer locked — waiting for the round to end…',
                  textAlign: TextAlign.center),
            ),
        ],
      ),
    );
  }
}

class _AnswerButton extends StatelessWidget {
  const _AnswerButton({required this.text, required this.enabled, required this.onTap});

  final String text;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return FilledButton.tonal(
      onPressed: enabled ? onTap : null,
      child: Text(text, textAlign: TextAlign.center),
    );
  }
}

class _RevealView extends StatelessWidget {
  const _RevealView({required this.game});

  final GameState game;

  @override
  Widget build(BuildContext context) {
    final correct = game.lastCorrect ?? false;
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          Icon(correct ? Icons.check_circle : Icons.cancel,
              color: correct ? Colors.green : Colors.red, size: 96),
          const SizedBox(height: 8),
          Text(correct ? 'Correct! +${game.lastPoints ?? 0}' : 'Wrong answer',
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 24),
          Expanded(child: _Scoreboard(players: game.scoreboard)),
        ],
      ),
    );
  }
}

class _FinishedView extends StatelessWidget {
  const _FinishedView({required this.players});

  final List<PlayerScore> players;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          Text('🏆 Final results',
              style: Theme.of(context).textTheme.headlineSmall),
          const SizedBox(height: 16),
          Expanded(child: _Scoreboard(players: players)),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () => context.go('/'),
              child: const Text('Back to home'),
            ),
          ),
        ],
      ),
    );
  }
}

class _Scoreboard extends StatelessWidget {
  const _Scoreboard({required this.players});

  final List<PlayerScore> players;

  @override
  Widget build(BuildContext context) {
    if (players.isEmpty) {
      return const Center(child: Text('No scores yet.'));
    }
    return ListView(
      children: [
        for (final p in players)
          ListTile(
            leading: CircleAvatar(child: Text('${p.rank}')),
            title: Text(p.username),
            trailing: Text('${p.score} pts',
                style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
      ],
    );
  }
}
