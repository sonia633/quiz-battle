import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../models/quiz.dart';
import '../../providers/data_providers.dart';

/// Single-player practice. The backend hides correct answers in the play
/// payload, so when correctness is available the run is graded; otherwise it
/// falls back to a study mode that reveals explanations.
class QuizPlayScreen extends ConsumerWidget {
  const QuizPlayScreen({super.key, required this.quizId});

  final int quizId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final detail = ref.watch(quizDetailProvider(quizId));
    return Scaffold(
      appBar: AppBar(title: const Text('Practice')),
      body: detail.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('Could not load the quiz.'),
              TextButton(
                onPressed: () => ref.invalidate(quizDetailProvider(quizId)),
                child: const Text('Retry'),
              ),
            ],
          ),
        ),
        data: (quiz) => quiz.questions.isEmpty
            ? const Center(child: Text('This quiz has no questions.'))
            : _Runner(quiz: quiz),
      ),
    );
  }
}

class _Runner extends StatefulWidget {
  const _Runner({required this.quiz});

  final QuizDetail quiz;

  @override
  State<_Runner> createState() => _RunnerState();
}

class _RunnerState extends State<_Runner> {
  int _index = 0;
  int _correct = 0;
  int? _selected;
  bool _locked = false;

  bool get _graded =>
      widget.quiz.questions.first.answers.any((a) => a.correct != null);

  Question get _q => widget.quiz.questions[_index];

  void _select(Answer answer) {
    if (_locked) return;
    setState(() {
      _selected = answer.id;
      _locked = true;
      if (answer.correct == true) _correct++;
    });
  }

  void _next() {
    if (_index + 1 >= widget.quiz.questions.length) {
      _showSummary();
      return;
    }
    setState(() {
      _index++;
      _selected = null;
      _locked = false;
    });
  }

  void _showSummary() {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Practice complete'),
        content: Text(_graded
            ? 'You scored $_correct / ${widget.quiz.questions.length}.'
            : 'You reviewed ${widget.quiz.questions.length} questions.\n'
                'Host a battle to be scored live!'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              context.pop();
            },
            child: const Text('Done'),
          ),
        ],
      ),
    );
  }

  Color? _tileColor(Answer a, ColorScheme scheme) {
    if (!_locked || !_graded) return null;
    if (a.correct == true) return scheme.primaryContainer;
    if (a.id == _selected) return scheme.errorContainer;
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final total = widget.quiz.questions.length;
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          LinearProgressIndicator(value: (_index + 1) / total),
          const SizedBox(height: 8),
          Text('Question ${_index + 1} of $total'),
          const SizedBox(height: 16),
          Text(_q.text,
              style: Theme.of(context)
                  .textTheme
                  .titleLarge
                  ?.copyWith(fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          Expanded(
            child: ListView(
              children: [
                for (final a in _q.answers)
                  Card(
                    color: _tileColor(a, scheme),
                    child: ListTile(
                      title: Text(a.text),
                      leading: Radio<int>(
                        value: a.id,
                        groupValue: _selected,
                        onChanged: _locked ? null : (_) => _select(a),
                      ),
                      onTap: _locked ? null : () => _select(a),
                    ),
                  ),
                if (_locked && _q.explanation != null)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Text('💡 ${_q.explanation!}',
                        style: const TextStyle(fontStyle: FontStyle.italic)),
                  ),
              ],
            ),
          ),
          FilledButton(
            onPressed: _locked ? _next : null,
            child: Text(_index + 1 >= total ? 'Finish' : 'Next'),
          ),
        ],
      ),
    );
  }
}
