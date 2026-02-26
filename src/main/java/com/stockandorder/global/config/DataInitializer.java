package com.stockandorder.global.config;

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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.existsByLoginId("admin")) {
            return;
        }

        Member admin = Member.create(
                "admin",
                passwordEncoder.encode("admin1234"),
                "관리자",
                null,
                Role.ADMIN
        );
        memberRepository.save(admin);

        log.info("=================================================");
        log.info("초기 관리자 계정이 생성되었습니다.");
        log.info("아이디: admin / 비밀번호: admin1234");
        log.info("=================================================");
    }
}
