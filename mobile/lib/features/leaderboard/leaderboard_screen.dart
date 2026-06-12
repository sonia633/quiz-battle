import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../models/leaderboard.dart';
import '../../providers/auth_controller.dart';
import '../../providers/data_providers.dart';

class LeaderboardScreen extends ConsumerWidget {
  const LeaderboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return DefaultTabController(
      length: 3,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Ranking'),
          bottom: const TabBar(tabs: [
            Tab(text: 'Global'),
            Tab(text: 'Weekly'),
            Tab(text: 'Monthly'),
          ]),
        ),
        body: const TabBarView(children: [
          _RankingList(scope: 'global'),
          _RankingList(scope: 'weekly'),
          _RankingList(scope: 'monthly'),
        ]),
      ),
    );
  }
}

class _RankingList extends ConsumerWidget {
  const _RankingList({required this.scope});

  final String scope;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ranking = ref.watch(leaderboardProvider(scope));
    final myId = ref.watch(authControllerProvider).user?.id;

    return ranking.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (e, _) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Could not load the ranking.'),
            TextButton(
              onPressed: () => ref.invalidate(leaderboardProvider(scope)),
              child: const Text('Retry'),
            ),
          ],
        ),
      ),
      data: (entries) {
        if (entries.isEmpty) {
          return const Center(child: Text('No ranked players yet.'));
        }
        return RefreshIndicator(
          onRefresh: () async => ref.refresh(leaderboardProvider(scope).future),
          child: ListView.builder(
            itemCount: entries.length,
            itemBuilder: (_, i) => _RankTile(entry: entries[i], isMe: entries[i].userId == myId),
          ),
        );
      },
    );
  }
}

class _RankTile extends StatelessWidget {
  const _RankTile({required this.entry, required this.isMe});

  final RankingEntry entry;
  final bool isMe;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final medal = switch (entry.rank) {
      1 => '🥇',
      2 => '🥈',
      3 => '🥉',
      _ => '#${entry.rank}',
    };
    return ListTile(
      tileColor: isMe ? scheme.primaryContainer.withOpacity(0.4) : null,
      leading: SizedBox(
        width: 36,
        child: Center(
            child: Text(medal, style: const TextStyle(fontSize: 18))),
      ),
      title: Text(isMe ? 'You' : 'Player #${entry.userId}',
          style: TextStyle(fontWeight: isMe ? FontWeight.bold : FontWeight.normal)),
      subtitle: Text('Level ${entry.level} · ${entry.score} pts'),
      trailing: Text('${entry.xp} XP',
          style: const TextStyle(fontWeight: FontWeight.bold)),
    );
  }
}
