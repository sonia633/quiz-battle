import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:quiz_battle/features/auth/login_screen.dart';

void main() {
  testWidgets('renders the sign-in form', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: LoginScreen()),
      ),
    );

    expect(find.text('Quiz Battle'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Username or email'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Password'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Sign in'), findsOneWidget);
  });

  testWidgets('shows validation errors when submitted empty', (tester) async {
    await tester.pumpWidget(
      const ProviderScope(
        child: MaterialApp(home: LoginScreen()),
      ),
    );

    await tester.tap(find.widgetWithText(FilledButton, 'Sign in'));
    await tester.pump();

    // Both required fields surface the validation message.
    expect(find.text('Required'), findsNWidgets(2));
  });
}
