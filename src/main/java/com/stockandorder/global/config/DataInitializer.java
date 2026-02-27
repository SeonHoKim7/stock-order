package com.stockandorder.global.config;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.category.repository.CategoryRepository;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initDefaultCategory();
    }

    private void initAdmin() {
        if (memberRepository.existsByLoginId("admin")) {
            return;
        }
        memberRepository.save(Member.create(
                "admin",
                passwordEncoder.encode("admin1234"),
                "관리자",
                null,
                Role.ADMIN
        ));
        log.info("초기 관리자 계정 생성 완료 - 아이디: admin / 비밀번호: admin1234");
    }

    private void initDefaultCategory() {
        if (categoryRepository.existsByName("미분류")) {
            return;
        }
        categoryRepository.save(Category.create("미분류", "카테고리 미지정 상품"));
        log.info("기본 카테고리 '미분류' 생성 완료");
    }
}
