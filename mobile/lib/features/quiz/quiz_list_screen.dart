import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../providers/data_providers.dart';
import '../../providers/providers.dart';

class QuizListScreen extends ConsumerWidget {
  const QuizListScreen({super.key, this.categoryId, this.categoryName});

  final int? categoryId;
  final String? categoryName;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final quizzes = ref.watch(quizzesProvider(categoryId));
    return Scaffold(
      appBar: AppBar(title: Text(categoryName ?? 'Quizzes')),
      body: quizzes.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Could not load quizzes.'),
              TextButton(
                onPressed: () => ref.invalidate(quizzesProvider(categoryId)),
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
        data: (items) {
          if (items.isEmpty) {
            return const Center(child: Text('No published quizzes here yet.'));
          }
          return ListView.separated(
            padding: const EdgeInsets.all(12),
            itemCount: items.length,
            separatorBuilder: (_, __) => const SizedBox(height: 8),
            itemBuilder: (_, i) {
              final q = items[i];
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(q.title,
                          style: Theme.of(context)
                              .textTheme
                              .titleMedium
                              ?.copyWith(fontWeight: FontWeight.bold)),
                      if (q.description != null) ...[
                        const SizedBox(height: 4),
                        Text(q.description!,
                            maxLines: 2, overflow: TextOverflow.ellipsis),
                      ],
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        children: [
                          Chip(label: Text(q.difficulty)),
                          Chip(label: Text('${q.questionCount} questions')),
                          Chip(label: Text(q.category)),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton.icon(
                            onPressed: () => context.push('/play/${q.id}'),
                            icon: const Icon(Icons.play_arrow),
                            label: const Text('Practice'),
                          ),
                          const SizedBox(width: 8),
                          FilledButton.icon(
                            onPressed: () => _hostBattle(context, ref, q.id),
                            icon: const Icon(Icons.groups),
                            label: const Text('Host battle'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }

  Future<void> _hostBattle(BuildContext context, WidgetRef ref, int quizId) async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      final room = await ref.read(quizServiceProvider).createRoom(quizId);
      if (context.mounted) context.push('/room/${room.code}');
    } catch (_) {
      messenger.showSnackBar(
        const SnackBar(content: Text('Could not create the room.')),
      );
    }
  }
}
