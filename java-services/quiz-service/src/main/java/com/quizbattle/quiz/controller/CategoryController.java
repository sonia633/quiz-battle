package com.quizbattle.quiz.controller;

import com.quizbattle.quiz.dto.QuizDtos.CategoryRequest;
import com.quizbattle.quiz.dto.QuizDtos.CategoryResponse;
import com.quizbattle.quiz.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "List all quiz categories")
    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @Operation(summary = "Create a category (admin/moderator)")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MODERATOR')")
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @Operation(summary = "Update a category (admin/moderator)")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MODERATOR')")
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(id, request);
    }

    @Operation(summary = "Delete a category (admin)")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
