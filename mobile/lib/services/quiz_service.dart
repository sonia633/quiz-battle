import '../core/api_client.dart';
import '../models/game.dart';
import '../models/quiz.dart';

/// Categories, quizzes and room lifecycle (REST part of the quiz-service).
class QuizService {
  QuizService(this._api);

  final ApiClient _api;

  Future<List<Category>> categories() async {
    final res = await _api.dio.get('/api/categories');
    return (res.data as List)
        .map((e) => Category.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<QuizSummary>> quizzes({int? categoryId}) async {
    final res = await _api.dio.get(
      '/api/quizzes',
      queryParameters: {if (categoryId != null) 'categoryId': categoryId},
    );
    final content = (res.data as Map<String, dynamic>)['content'] as List? ?? [];
    return content
        .map((e) => QuizSummary.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<QuizDetail> quizForPlaying(int id) async {
    final res = await _api.dio.get('/api/quizzes/$id');
    return QuizDetail.fromJson(res.data as Map<String, dynamic>);
  }

  Future<RoomInfo> createRoom(int quizId, {int? maxPlayers}) async {
    final res = await _api.dio.post('/api/rooms', data: {
      'quizId': quizId,
      if (maxPlayers != null) 'maxPlayers': maxPlayers,
    });
    return RoomInfo.fromJson(res.data as Map<String, dynamic>);
  }

  Future<RoomInfo> roomByCode(String code) async {
    final res = await _api.dio.get('/api/rooms/$code');
    return RoomInfo.fromJson(res.data as Map<String, dynamic>);
  }

  Future<void> startRoom(String code) => _api.dio.post('/api/rooms/$code/start');
}
