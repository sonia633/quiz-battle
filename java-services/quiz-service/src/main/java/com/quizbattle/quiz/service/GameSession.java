package com.quizbattle.quiz.service;

import com.quizbattle.quiz.entity.Question;
import com.quizbattle.quiz.entity.Quiz;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mutable in-memory state for one live battle. Thread-safe for the operations
 * the game loop performs (join, submit, advance). Persisted artifacts (rooms,
 * results) live in the database; this holds only the transient round state.
 */
public class GameSession {

    public record Player(Long userId, String username) {
    }

    private final String roomCode;
    private final Long quizId;
    private final int timePerQuestionSeconds;
    private final List<Question> questions;

    private final Map<Long, String> players = new ConcurrentHashMap<>();   // userId -> username
    private final Map<Long, Integer> scores = new ConcurrentHashMap<>();   // userId -> cumulative score
    private final Map<Long, Integer> correctCounts = new ConcurrentHashMap<>();
    private final Set<Long> answeredThisRound = ConcurrentHashMap.newKeySet();

    private volatile int currentIndex = -1;
    private volatile boolean started = false;

    public GameSession(String roomCode, Quiz quiz) {
        this.roomCode = roomCode;
        this.quizId = quiz.getId();
        this.timePerQuestionSeconds = quiz.getTimePerQuestion();
        this.questions = new ArrayList<>(quiz.getQuestions());
    }

    public synchronized void addPlayer(Long userId, String username) {
        players.put(userId, username);
        scores.putIfAbsent(userId, 0);
        correctCounts.putIfAbsent(userId, 0);
    }

    public synchronized void removePlayer(Long userId) {
        players.remove(userId);
    }

    public synchronized boolean start() {
        if (started || questions.isEmpty()) {
            return false;
        }
        started = true;
        return true;
    }

    /** Advances to the next question. Returns the question, or empty when done. */
    public synchronized Optional<Question> nextQuestion() {
        answeredThisRound.clear();
        currentIndex++;
        if (currentIndex >= questions.size()) {
            return Optional.empty();
        }
        return Optional.of(questions.get(currentIndex));
    }

    public Optional<Question> currentQuestion() {
        int idx = currentIndex;
        if (idx < 0 || idx >= questions.size()) {
            return Optional.empty();
        }
        return Optional.of(questions.get(idx));
    }

    /**
     * Records an answer and returns the points awarded (0 if wrong, late, the
     * player already answered, or the question id does not match the round).
     */
    public synchronized int submitAnswer(Long userId, Long questionId, Long answerId, long elapsedMillis) {
        Optional<Question> current = currentQuestion();
        if (current.isEmpty() || !players.containsKey(userId)) {
            return 0;
        }
        Question q = current.get();
        if (!q.getId().equals(questionId) || !answeredThisRound.add(userId)) {
            return 0;
        }

        boolean correct = q.getAnswers().stream()
                .anyMatch(a -> a.getId().equals(answerId) && a.isCorrect());
        if (!correct) {
            return 0;
        }

        int awarded = computeScore(q.getPoints(), elapsedMillis);
        scores.merge(userId, awarded, Integer::sum);
        correctCounts.merge(userId, 1, Integer::sum);
        return awarded;
    }

    /** Base points plus a speed bonus that decays linearly to 50% at the deadline. */
    private int computeScore(int basePoints, long elapsedMillis) {
        double limitMillis = timePerQuestionSeconds * 1000.0;
        double ratio = Math.min(1.0, Math.max(0.0, elapsedMillis / limitMillis));
        double factor = 1.0 - 0.5 * ratio;
        return (int) Math.round(basePoints * factor);
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isFinished() {
        return started && currentIndex >= questions.size();
    }

    public int totalQuestions() {
        return questions.size();
    }

    public int timePerQuestionSeconds() {
        return timePerQuestionSeconds;
    }

    public Long quizId() {
        return quizId;
    }

    public String roomCode() {
        return roomCode;
    }

    public Map<Long, String> players() {
        return Collections.unmodifiableMap(players);
    }

    public int scoreOf(Long userId) {
        return scores.getOrDefault(userId, 0);
    }

    public int correctCountOf(Long userId) {
        return correctCounts.getOrDefault(userId, 0);
    }

    /** Players ranked by descending score. */
    public List<Map.Entry<Long, Integer>> ranking() {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .toList();
    }
}
