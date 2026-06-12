package com.quizbattle.quiz.service;

import com.quizbattle.quiz.dto.QuizDtos.QuestionResponse;
import com.quizbattle.quiz.dto.RoomDtos.*;
import com.quizbattle.quiz.entity.*;
import com.quizbattle.quiz.exception.BadRequestException;
import com.quizbattle.quiz.exception.ResourceNotFoundException;
import com.quizbattle.quiz.repository.GameResultRepository;
import com.quizbattle.quiz.repository.GameRoomRepository;
import com.quizbattle.quiz.repository.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates multiplayer battles: room lifecycle, the per-question game loop
 * (driven by a {@link TaskScheduler}), scoring, and result persistence.
 *
 * <p>Game events are pushed to subscribers of {@code /topic/rooms/{code}}.
 */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GameRoomRepository roomRepository;
    private final QuizRepository quizRepository;
    private final GameResultRepository resultRepository;
    private final QuizMapper quizMapper;
    private final SimpMessagingTemplate messaging;
    private final TaskScheduler scheduler;
    private final LeaderboardPublisher leaderboardPublisher;

    /** Active sessions keyed by room code. */
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    public RoomService(GameRoomRepository roomRepository,
                       QuizRepository quizRepository,
                       GameResultRepository resultRepository,
                       QuizMapper quizMapper,
                       SimpMessagingTemplate messaging,
                       TaskScheduler scheduler,
                       LeaderboardPublisher leaderboardPublisher) {
        this.roomRepository = roomRepository;
        this.quizRepository = quizRepository;
        this.resultRepository = resultRepository;
        this.quizMapper = quizMapper;
        this.messaging = messaging;
        this.scheduler = scheduler;
        this.leaderboardPublisher = leaderboardPublisher;
    }

    @Transactional
    public RoomResponse createRoom(Long hostId, CreateRoomRequest req) {
        Quiz quiz = quizRepository.findWithQuestionsById(req.quizId())
                .orElseThrow(() -> ResourceNotFoundException.of("Quiz", req.quizId()));
        if (!quiz.isPublished()) {
            throw new BadRequestException("Cannot host an unpublished quiz");
        }

        GameRoom room = GameRoom.builder()
                .code(generateUniqueCode())
                .quiz(quiz)
                .hostId(hostId)
                .status(RoomStatus.WAITING)
                .maxPlayers(req.maxPlayers() != null ? req.maxPlayers() : 20)
                .build();
        roomRepository.save(room);

        sessions.put(room.getCode(), new GameSession(room.getCode(), quiz));
        log.info("Room {} created by host {} for quiz {}", room.getCode(), hostId, quiz.getId());
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public RoomResponse findByCode(String code) {
        return toResponse(requireRoom(code));
    }

    /** Adds a player to the room and broadcasts the updated scoreboard. */
    public void join(String code, Long userId, String username) {
        GameSession session = requireSession(code);
        session.addPlayer(userId, username);
        broadcast(code, EventType.PLAYER_JOINED, new PlayerScore(userId, username, 0, 0));
        broadcastScoreboard(session);
    }

    public void leave(String code, Long userId) {
        GameSession session = sessions.get(code);
        if (session != null) {
            String username = session.players().get(userId);
            session.removePlayer(userId);
            broadcast(code, EventType.PLAYER_LEFT, new PlayerScore(userId, username, 0, 0));
        }
    }

    @Transactional
    public void startGame(String code, Long requesterId) {
        GameRoom room = requireRoom(code);
        if (!room.getHostId().equals(requesterId)) {
            throw new BadRequestException("Only the host can start the game");
        }
        GameSession session = requireSession(code);
        if (!session.start()) {
            throw new BadRequestException("Game already started or has no questions");
        }
        room.setStatus(RoomStatus.IN_PROGRESS);

        broadcast(code, EventType.GAME_STARTED, Map.of(
                "totalQuestions", session.totalQuestions(),
                "timePerQuestion", session.timePerQuestionSeconds()));
        advance(code);
    }

    public int submitAnswer(String code, Long userId, AnswerSubmission submission) {
        GameSession session = requireSession(code);
        int awarded = session.submitAnswer(
                userId, submission.questionId(), submission.answerId(), submission.elapsedMillis());
        broadcast(code, EventType.ANSWER_RESULT, Map.of(
                "userId", userId,
                "correct", awarded > 0,
                "pointsAwarded", awarded,
                "totalScore", session.scoreOf(userId)));
        return awarded;
    }

    /**
     * Pushes the next question (correct answers stripped) and schedules the
     * round to close after the time limit. When questions run out, finishes.
     */
    private void advance(String code) {
        GameSession session = requireSession(code);
        Optional<Question> next = session.nextQuestion();
        if (next.isEmpty()) {
            finish(code);
            return;
        }

        QuestionResponse question = quizMapper.toQuestion(next.get(), false);
        broadcast(code, EventType.NEXT_QUESTION, question);

        Instant deadline = Instant.now().plusSeconds(session.timePerQuestionSeconds());
        scheduler.schedule(() -> {
            broadcastScoreboard(session);
            advance(code);
        }, deadline);
    }

    @Transactional
    public void finish(String code) {
        GameSession session = sessions.get(code);
        if (session == null) {
            return;
        }
        GameRoom room = requireRoom(code);
        room.setStatus(RoomStatus.FINISHED);

        List<Map.Entry<Long, Integer>> ranking = session.ranking();
        int rank = 1;
        for (Map.Entry<Long, Integer> entry : ranking) {
            Long userId = entry.getKey();
            int score = entry.getValue();
            int correct = session.correctCountOf(userId);
            int xp = calculateXp(score, rank, correct, session.totalQuestions());

            resultRepository.save(GameResult.builder()
                    .roomId(room.getId())
                    .userId(userId)
                    .quizId(session.quizId())
                    .score(score)
                    .correctAnswers(correct)
                    .totalQuestions(session.totalQuestions())
                    .finalRank(rank)
                    .xpEarned(xp)
                    .build());

            leaderboardPublisher.publishResult(
                    userId, session.quizId(), score, correct, session.totalQuestions(), rank, xp);
            rank++;
        }

        broadcastScoreboard(session);
        broadcast(code, EventType.GAME_FINISHED, buildScoreboard(session));
        sessions.remove(code);
        log.info("Room {} finished with {} players", code, ranking.size());
    }

    /** XP = score/10 + win bonus + accuracy bonus. */
    private int calculateXp(int score, int rank, int correct, int total) {
        int base = score / 10;
        int winBonus = rank == 1 ? 50 : 0;
        int accuracyBonus = total > 0 ? (int) (50.0 * correct / total) : 0;
        return base + winBonus + accuracyBonus;
    }

    private void broadcastScoreboard(GameSession session) {
        broadcast(session.roomCode(), EventType.SCOREBOARD, buildScoreboard(session));
    }

    private Scoreboard buildScoreboard(GameSession session) {
        List<PlayerScore> players = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, Integer> entry : session.ranking()) {
            String username = session.players().getOrDefault(entry.getKey(), "player");
            players.add(new PlayerScore(entry.getKey(), username, entry.getValue(), rank++));
        }
        return new Scoreboard(session.roomCode(), players);
    }

    private void broadcast(String code, EventType type, Object payload) {
        messaging.convertAndSend("/topic/rooms/" + code, GameEvent.of(type, payload));
    }

    private GameRoom requireRoom(String code) {
        return roomRepository.findByCode(code)
                .orElseThrow(() -> ResourceNotFoundException.of("Room", code));
    }

    private GameSession requireSession(String code) {
        GameSession session = sessions.get(code);
        if (session == null) {
            throw new BadRequestException("Room is not active: " + code);
        }
        return session;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
            }
            String code = sb.toString();
            if (!roomRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate a unique room code");
    }

    private RoomResponse toResponse(GameRoom room) {
        return new RoomResponse(
                room.getId(), room.getCode(), room.getQuiz().getId(),
                room.getQuiz().getTitle(), room.getHostId(),
                room.getStatus().name(), room.getMaxPlayers());
    }
}
