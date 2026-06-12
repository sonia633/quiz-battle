package com.quizbattle.quiz.service;

import com.quizbattle.quiz.dto.QuizDtos.CategoryRequest;
import com.quizbattle.quiz.dto.QuizDtos.CategoryResponse;
import com.quizbattle.quiz.entity.Category;
import com.quizbattle.quiz.exception.ConflictException;
import com.quizbattle.quiz.exception.ResourceNotFoundException;
import com.quizbattle.quiz.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        if (categoryRepository.existsByName(req.name())) {
            throw new ConflictException("Category already exists: " + req.name());
        }
        Category category = categoryRepository.save(Category.builder()
                .name(req.name())
                .description(req.description())
                .icon(req.icon())
                .build());
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
        category.setName(req.name());
        category.setDescription(req.description());
        category.setIcon(req.icon());
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw ResourceNotFoundException.of("Category", id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIcon());
    }
}
