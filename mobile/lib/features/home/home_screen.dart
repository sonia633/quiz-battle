import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../models/quiz.dart';
import '../../providers/data_providers.dart';
import '../leaderboard/leaderboard_screen.dart';
import '../profile/profile_screen.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  int _index = 0;

  static const _tabs = [_DiscoverTab(), LeaderboardScreen(), ProfileScreen()];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _tabs),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.explore_outlined), selectedIcon: Icon(Icons.explore), label: 'Discover'),
          NavigationDestination(icon: Icon(Icons.leaderboard_outlined), selectedIcon: Icon(Icons.leaderboard), label: 'Ranking'),
          NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
        ],
      ),
    );
  }
}

class _DiscoverTab extends ConsumerWidget {
  const _DiscoverTab();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final categories = ref.watch(categoriesProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Discover'),
        actions: [
          IconButton(
            tooltip: 'Join a room',
            icon: const Icon(Icons.meeting_room_outlined),
            onPressed: () => context.push('/join'),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/join'),
        icon: const Icon(Icons.qr_code_2),
        label: const Text('Join battle'),
      ),
      body: RefreshIndicator(
        onRefresh: () async => ref.refresh(categoriesProvider.future),
        child: categories.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => _ErrorView(
            message: 'Could not load categories.',
            onRetry: () => ref.invalidate(categoriesProvider),
          ),
          data: (items) => GridView.builder(
            padding: const EdgeInsets.all(16),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.3,
            ),
            itemCount: items.length,
            itemBuilder: (_, i) => _CategoryCard(category: items[i]),
          ),
        ),
      ),
    );
  }
}

class _CategoryCard extends StatelessWidget {
  const _CategoryCard({required this.category});

  final Category category;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.push(
            '/quizzes?categoryId=${category.id}&name=${Uri.encodeComponent(category.name)}'),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              CircleAvatar(
                backgroundColor: theme.colorScheme.primaryContainer,
                child: Icon(_iconFor(category.icon),
                    color: theme.colorScheme.onPrimaryContainer),
              ),
              Text(category.name,
                  style: theme.textTheme.titleMedium
                      ?.copyWith(fontWeight: FontWeight.bold)),
            ],
          ),
        ),
      ),
    );
  }

  IconData _iconFor(String? name) {
    switch (name) {
      case 'code':
        return Icons.code;
      case 'calculate':
        return Icons.calculate;
      case 'science':
        return Icons.science;
      case 'history_edu':
        return Icons.history_edu;
      case 'public':
        return Icons.public;
      case 'sports_soccer':
        return Icons.sports_soccer;
      default:
        return Icons.lightbulb_outline;
    }
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        const SizedBox(height: 120),
        const Icon(Icons.cloud_off, size: 48),
        const SizedBox(height: 12),
        Center(child: Text(message)),
        const SizedBox(height: 12),
        Center(
          child: OutlinedButton(onPressed: onRetry, child: const Text('Retry')),
        ),
      ],
    );
  }
}
