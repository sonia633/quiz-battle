package com.quizbattle.quiz.service;

import com.quizbattle.quiz.dto.QuizDtos.*;
import com.quizbattle.quiz.entity.*;
import com.quizbattle.quiz.exception.BadRequestException;
import com.quizbattle.quiz.exception.ResourceNotFoundException;
import com.quizbattle.quiz.repository.CategoryRepository;
import com.quizbattle.quiz.repository.QuizRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD and publishing for quizzes. Admin/moderator gated at the controller. */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final CategoryRepository categoryRepository;
    private final QuizMapper mapper;

    public QuizService(QuizRepository quizRepository,
                       CategoryRepository categoryRepository,
                       QuizMapper mapper) {
        this.quizRepository = quizRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<QuizSummary> listPublished(Long categoryId, Pageable pageable) {
        Page<Quiz> page = (categoryId == null)
                ? quizRepository.findByPublishedTrue(pageable)
                : quizRepository.findByCategoryIdAndPublishedTrue(categoryId, pageable);
        return page.map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public QuizDetail getForEditing(Long id) {
        return mapper.toDetail(loadWithQuestions(id), true);
    }

    /** Used by clients about to play — correct answers are hidden. */
    @Transactional(readOnly = true)
    public QuizDetail getForPlaying(Long id) {
        return mapper.toDetail(loadWithQuestions(id), false);
    }

    @Transactional
    public QuizDetail create(CreateQuizRequest req, Long authorId) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", req.categoryId()));

        Quiz quiz = Quiz.builder()
                .title(req.title())
                .description(req.description())
                .category(category)
                .difficulty(req.difficulty())
                .timePerQuestion(req.timePerQuestion())
                .authorId(authorId)
                .published(false)
                .build();

        req.questions().forEach(qr -> quiz.addQuestion(buildQuestion(qr)));
        return mapper.toDetail(quizRepository.save(quiz), true);
    }

    @Transactional
    public QuizDetail update(Long id, CreateQuizRequest req) {
        Quiz quiz = loadWithQuestions(id);
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", req.categoryId()));

        quiz.setTitle(req.title());
        quiz.setDescription(req.description());
        quiz.setCategory(category);
        quiz.setDifficulty(req.difficulty());
        quiz.setTimePerQuestion(req.timePerQuestion());

        // Replace the question set wholesale — simplest correct semantics for an edit.
        quiz.getQuestions().clear();
        req.questions().forEach(qr -> quiz.addQuestion(buildQuestion(qr)));

        return mapper.toDetail(quiz, true);
    }

    @Transactional
    public void setPublished(Long id, boolean published) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Quiz", id));
        quiz.setPublished(published);
    }

    @Transactional
    public void delete(Long id) {
        if (!quizRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Quiz", id);
        }
        quizRepository.deleteById(id);
    }

    private Question buildQuestion(QuestionRequest qr) {
        long correctCount = qr.answers().stream().filter(AnswerRequest::correct).count();
        if (qr.type() == QuestionType.TRUE_FALSE && qr.answers().size() != 2) {
            throw new BadRequestException("True/False questions need exactly 2 answers");
        }
        if (correctCount != 1) {
            throw new BadRequestException("Each question must have exactly one correct answer");
        }

        Question question = Question.builder()
                .text(qr.text())
                .type(qr.type())
                .points(qr.points())
                .explanation(qr.explanation())
                .build();
        qr.answers().forEach(ar -> question.addAnswer(
                Answer.builder().text(ar.text()).correct(ar.correct()).build()));
        return question;
    }

    private Quiz loadWithQuestions(Long id) {
        return quizRepository.findWithQuestionsById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Quiz", id));
    }
}
