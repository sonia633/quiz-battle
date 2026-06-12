package com.quizbattle.quiz.controller;

import com.quizbattle.quiz.dto.QuizDtos.CreateQuizRequest;
import com.quizbattle.quiz.dto.QuizDtos.QuizDetail;
import com.quizbattle.quiz.dto.QuizDtos.QuizSummary;
import com.quizbattle.quiz.security.SecurityUtils;
import com.quizbattle.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Quizzes")
@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @Operation(summary = "List published quizzes, optionally filtered by category")
    @GetMapping
    public Page<QuizSummary> list(@RequestParam(required = false) Long categoryId,
                                  @PageableDefault(size = 20) Pageable pageable) {
        return quizService.listPublished(categoryId, pageable);
    }

    @Operation(summary = "Get a quiz for playing (correct answers hidden)")
    @GetMapping("/{id}")
    public QuizDetail getForPlaying(@PathVariable Long id) {
        return quizService.getForPlaying(id);
    }

    @Operation(summary = "Get a quiz with answers for editing (admin/moderator)")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MODERATOR')")
    @GetMapping("/{id}/edit")
    public QuizDetail getForEditing(@PathVariable Long id) {
        return quizService.getForEditing(id);
    }

    @Operation(summary = "Create a quiz (admin/moderator)")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MODERATOR')")
    @PostMapping
    public ResponseEntity<QuizDetail> create(@Valid @RequestBody CreateQuizRequest request) {
        QuizDetail created = quizService.create(request, SecurityUtils.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update a quiz (admin/moderator)")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MODERATOR')")
    @PutMapping("/{id}")
    public QuizDetail update(@PathVariable Long id, @Valid @RequestBody CreateQuizRequest request) {
        return quizService.update(id, request);
    }

    @Operation(summary = "Publish or unpublish a quiz (admin/moderator)")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MODERATOR')")
    @PatchMapping("/{id}/publish")
    public ResponseEntity<Void> setPublished(@PathVariable Long id, @RequestParam boolean published) {
        quizService.setPublished(id, published);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a quiz (admin)")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quizService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
