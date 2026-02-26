package com.stockandorder.domain.category.entity;

import com.stockandorder.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    public static Category create(String name, String description) {
        Category category = new Category();
        category.name = name;
        category.description = description;
        return category;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
