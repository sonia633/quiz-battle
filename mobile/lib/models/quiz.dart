class Category {
  const Category({
    required this.id,
    required this.name,
    this.description,
    this.icon,
  });

  final int id;
  final String name;
  final String? description;
  final String? icon;

  factory Category.fromJson(Map<String, dynamic> json) => Category(
        id: json['id'] as int,
        name: json['name'] as String,
        description: json['description'] as String?,
        icon: json['icon'] as String?,
      );
}

class QuizSummary {
  const QuizSummary({
    required this.id,
    required this.title,
    this.description,
    required this.category,
    required this.difficulty,
    required this.questionCount,
  });

  final int id;
  final String title;
  final String? description;
  final String category;
  final String difficulty;
  final int questionCount;

  factory QuizSummary.fromJson(Map<String, dynamic> json) => QuizSummary(
        id: json['id'] as int,
        title: json['title'] as String,
        description: json['description'] as String?,
        category: json['category'] as String,
        difficulty: json['difficulty'] as String,
        questionCount: (json['questionCount'] as num?)?.toInt() ?? 0,
      );
}

class Answer {
  const Answer({required this.id, required this.text, this.correct});

  final int id;
  final String text;

  /// Null while a quiz is being played (the server hides correctness).
  final bool? correct;

  factory Answer.fromJson(Map<String, dynamic> json) => Answer(
        id: json['id'] as int,
        text: json['text'] as String,
        correct: json['correct'] as bool?,
      );
}

class Question {
  const Question({
    required this.id,
    required this.text,
    required this.type,
    required this.points,
    this.explanation,
    required this.answers,
  });

  final int id;
  final String text;
  final String type;
  final int points;
  final String? explanation;
  final List<Answer> answers;

  factory Question.fromJson(Map<String, dynamic> json) => Question(
        id: json['id'] as int,
        text: json['text'] as String,
        type: json['type'] as String,
        points: (json['points'] as num?)?.toInt() ?? 100,
        explanation: json['explanation'] as String?,
        answers: ((json['answers'] as List?) ?? const [])
            .map((e) => Answer.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

class QuizDetail {
  const QuizDetail({
    required this.id,
    required this.title,
    required this.category,
    required this.difficulty,
    required this.timePerQuestion,
    required this.questions,
  });

  final int id;
  final String title;
  final String category;
  final String difficulty;
  final int timePerQuestion;
  final List<Question> questions;

  factory QuizDetail.fromJson(Map<String, dynamic> json) => QuizDetail(
        id: json['id'] as int,
        title: json['title'] as String,
        category: json['category'] as String,
        difficulty: json['difficulty'] as String,
        timePerQuestion: (json['timePerQuestion'] as num?)?.toInt() ?? 20,
        questions: ((json['questions'] as List?) ?? const [])
            .map((e) => Question.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}
