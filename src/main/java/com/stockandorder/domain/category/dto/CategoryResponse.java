package com.stockandorder.domain.category.dto;

import com.stockandorder.domain.category.entity.Category;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CategoryResponse {

    private final Long categoryId;
    private final String name;
    private final String description;
    private final LocalDateTime createdAt;

    private CategoryResponse(Category category) {
        this.categoryId = category.getCategoryId();
        this.name = category.getName();
        this.description = category.getDescription();
        this.createdAt = category.getCreatedAt();
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category);
    }
}
