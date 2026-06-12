import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'features/auth/login_screen.dart';
import 'features/auth/register_screen.dart';
import 'features/home/home_screen.dart';
import 'features/quiz/quiz_list_screen.dart';
import 'features/quiz/quiz_play_screen.dart';
import 'features/room/join_room_screen.dart';
import 'features/room/room_screen.dart';
import 'providers/auth_controller.dart';

/// Bridges Riverpod auth-state changes into a [Listenable] for go_router.
class _AuthRefresh extends ChangeNotifier {
  _AuthRefresh(Ref ref) {
    ref.listen(authControllerProvider, (_, __) => notifyListeners());
  }
}

final routerProvider = Provider<GoRouter>((ref) {
  final refresh = _AuthRefresh(ref);
  return GoRouter(
    initialLocation: '/',
    refreshListenable: refresh,
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      final loc = state.matchedLocation;
      final onAuthPage = loc == '/login' || loc == '/register';

      if (auth.status == AuthStatus.unknown) return null; // splash handles it
      if (auth.status == AuthStatus.unauthenticated) {
        return onAuthPage ? null : '/login';
      }
      // authenticated
      return onAuthPage ? '/' : null;
    },
    routes: [
      GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
      GoRoute(path: '/register', builder: (_, __) => const RegisterScreen()),
      GoRoute(path: '/', builder: (_, __) => const HomeScreen()),
      GoRoute(
        path: '/quizzes',
        builder: (_, state) {
          final categoryId = int.tryParse(state.uri.queryParameters['categoryId'] ?? '');
          final categoryName = state.uri.queryParameters['name'];
          return QuizListScreen(categoryId: categoryId, categoryName: categoryName);
        },
      ),
      GoRoute(
        path: '/play/:quizId',
        builder: (_, state) =>
            QuizPlayScreen(quizId: int.parse(state.pathParameters['quizId']!)),
      ),
      GoRoute(path: '/join', builder: (_, __) => const JoinRoomScreen()),
      GoRoute(
        path: '/room/:code',
        builder: (_, state) => RoomScreen(roomCode: state.pathParameters['code']!),
      ),
    ],
  );
});

class QuizBattleApp extends ConsumerWidget {
  const QuizBattleApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final router = ref.watch(routerProvider);

    final theme = ThemeData(
      colorScheme: ColorScheme.fromSeed(
        seedColor: const Color(0xFF6C4DF6),
        brightness: Brightness.light,
      ),
      useMaterial3: true,
    );

    // While bootstrapping the session, show a splash instead of the router.
    if (auth.status == AuthStatus.unknown) {
      return MaterialApp(
        theme: theme,
        debugShowCheckedModeBanner: false,
        home: const Scaffold(body: Center(child: CircularProgressIndicator())),
      );
    }

    return MaterialApp.router(
      title: 'Quiz Battle',
      theme: theme,
      debugShowCheckedModeBanner: false,
      routerConfig: router,
    );
  }
}
