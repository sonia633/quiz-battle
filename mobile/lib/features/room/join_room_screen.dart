import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class JoinRoomScreen extends ConsumerStatefulWidget {
  const JoinRoomScreen({super.key});

  @override
  ConsumerState<JoinRoomScreen> createState() => _JoinRoomScreenState();
}

class _JoinRoomScreenState extends ConsumerState<JoinRoomScreen> {
  final _code = TextEditingController();

  @override
  void dispose() {
    _code.dispose();
    super.dispose();
  }

  void _join() {
    final code = _code.text.trim().toUpperCase();
    if (code.isEmpty) return;
    context.push('/room/$code');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Join a battle')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 380),
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.qr_code_2, size: 72),
                const SizedBox(height: 16),
                const Text('Enter the 6-character room code shared by the host.'),
                const SizedBox(height: 16),
                TextField(
                  controller: _code,
                  textCapitalization: TextCapitalization.characters,
                  textAlign: TextAlign.center,
                  maxLength: 6,
                  style: const TextStyle(fontSize: 28, letterSpacing: 8),
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    counterText: '',
                    hintText: 'ABC123',
                  ),
                  onSubmitted: (_) => _join(),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(onPressed: _join, child: const Text('Join')),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
