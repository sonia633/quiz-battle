import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../models/leaderboard.dart';
import '../../providers/auth_controller.dart';
import '../../providers/data_providers.dart';

/// Player dashboard: games played, wins, accuracy, XP, rank/level.
class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(authControllerProvider).user;
    final stats = ref.watch(myStatsProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Profile'),
        actions: [
          IconButton(
            tooltip: 'Sign out',
            icon: const Icon(Icons.logout),
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async => ref.refresh(myStatsProvider.future),
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _Header(name: user?.displayName ?? 'Player', roles: user?.roles ?? const {}),
            const SizedBox(height: 16),
            stats.when(
              loading: () => const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
              error: (e, _) => const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: Text('Could not load your stats yet.')),
              ),
              data: (s) => s == null ? const SizedBox() : _StatsBody(stats: s),
            ),
          ],
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.name, required this.roles});

  final String name;
  final Set<String> roles;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      children: [
        CircleAvatar(
          radius: 32,
          backgroundColor: theme.colorScheme.primaryContainer,
          child: Text(name.isNotEmpty ? name[0].toUpperCase() : '?',
              style: theme.textTheme.headlineSmall),
        ),
        const SizedBox(width: 16),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(name, style: theme.textTheme.titleLarge),
            const SizedBox(height: 4),
            Wrap(
              spacing: 6,
              children: [
                for (final r in roles)
                  Chip(
                    label: Text(r, style: const TextStyle(fontSize: 11)),
                    visualDensity: VisualDensity.compact,
                  ),
              ],
            ),
          ],
        ),
      ],
    );
  }
}

class _StatsBody extends StatelessWidget {
  const _StatsBody({required this.stats});

  final PlayerStats stats;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GridView.count(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisCount: 2,
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          childAspectRatio: 1.6,
          children: [
            _StatCard(label: 'Games', value: '${stats.gamesPlayed}', icon: Icons.sports_esports),
            _StatCard(label: 'Wins', value: '${stats.wins}', icon: Icons.emoji_events),
            _StatCard(label: 'Accuracy', value: '${(stats.accuracy * 100).toStringAsFixed(0)}%', icon: Icons.track_changes),
            _StatCard(label: 'Level', value: '${stats.level}', icon: Icons.military_tech),
          ],
        ),
        const SizedBox(height: 16),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Progress', style: TextStyle(fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                Text('${stats.xp} XP · ${stats.totalScore} total points'),
                const SizedBox(height: 16),
                SizedBox(height: 140, child: _AccuracyGauge(accuracy: stats.accuracy)),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.label, required this.value, required this.icon});

  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            Icon(icon, color: theme.colorScheme.primary),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(value, style: theme.textTheme.headlineSmall),
                Text(label, style: theme.textTheme.bodySmall),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// Simple radial gauge for accuracy using a pie chart.
class _AccuracyGauge extends StatelessWidget {
  const _AccuracyGauge({required this.accuracy});

  final double accuracy;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final pct = (accuracy * 100).clamp(0, 100).toDouble();
    return PieChart(
      PieChartData(
        startDegreeOffset: -90,
        sectionsSpace: 0,
        centerSpaceRadius: 44,
        sections: [
          PieChartSectionData(value: pct, color: scheme.primary, radius: 18, showTitle: false),
          PieChartSectionData(value: 100 - pct, color: scheme.surfaceContainerHighest, radius: 18, showTitle: false),
        ],
      ),
    );
  }
}
