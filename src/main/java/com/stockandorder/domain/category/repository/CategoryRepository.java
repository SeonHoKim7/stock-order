package com.stockandorder.domain.category.repository;

import com.stockandorder.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    // 수정 시 자기 자신을 제외하고 이름 중복 체크
    boolean existsByNameAndCategoryIdNot(String name, Long categoryId);
}
