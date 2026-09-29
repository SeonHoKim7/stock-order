package com.stockandorder.global.config;

import com.stockandorder.domain.category.entity.Category;
import com.stockandorder.domain.category.repository.CategoryRepository;
import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init-admin.password:}")
    private String initAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        initAdmin();
        initDefaultCategory();
    }

    private void initAdmin() {
        if (memberRepository.existsByLoginId(Member.INITIAL_ADMIN_LOGIN_ID)) {
            return;
        }
        // 공개 저장소에 비밀번호를 두지 않기 위해 환경변수로만 받는다.
        // 미설정 시 기본값으로 만들지 않고 생성 자체를 건너뛴다(추측 가능한 관리자 계정이 생기는 것보다 안전).
        if (!StringUtils.hasText(initAdminPassword)) {
            log.warn("INIT_ADMIN_PASSWORD 미설정 - 초기 관리자 계정을 생성하지 않습니다.");
            return;
        }
        memberRepository.save(Member.create(
                Member.INITIAL_ADMIN_LOGIN_ID,
                passwordEncoder.encode(initAdminPassword),
                "관리자",
                null,
                Role.ADMIN
        ));
        log.info("초기 관리자 계정 생성 완료 - 아이디: admin");
    }

    private void initDefaultCategory() {
        if (categoryRepository.existsByName("미분류")) {
            return;
        }
        categoryRepository.save(Category.create("미분류", "카테고리 미지정 상품"));
        log.info("기본 카테고리 '미분류' 생성 완료");
    }
}
