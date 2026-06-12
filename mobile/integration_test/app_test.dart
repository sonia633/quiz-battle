// Integration smoke test. Run on a device/emulator with:
//   flutter test integration_test/app_test.dart
//
// With no stored session the app should settle on the login screen.
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:quiz_battle/app.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('cold start lands on the login screen', (tester) async {
    await tester.pumpWidget(const ProviderScope(child: QuizBattleApp()));
    await tester.pumpAndSettle();

    expect(find.text('Quiz Battle'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
  });
}
