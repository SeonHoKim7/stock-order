package com.stockandorder.global.auth;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import com.stockandorder.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("존재하는 loginId로 조회하면 CustomUserDetails를 반환한다")
    void loadUserByUsername_existingLoginId_returnsCustomUserDetails() {
        Member member = Member.create("staff01", "encodedPw", "홍길동", null, Role.STAFF);
        given(memberRepository.findByLoginId("staff01")).willReturn(Optional.of(member));

        UserDetails result = customUserDetailsService.loadUserByUsername("staff01");

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("staff01");
    }

    @Test
    @DisplayName("존재하지 않는 loginId로 조회하면 UsernameNotFoundException이 발생한다")
    void loadUserByUsername_notExistingLoginId_throwsException() {
        given(memberRepository.findByLoginId("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("반환된 UserDetails의 정보가 Member와 일치한다")
    void loadUserByUsername_returnedUserDetails_matchesMember() {
        Member member = Member.create("manager01", "encodedPw", "김매니저", "mgr@test.com", Role.MANAGER);
        given(memberRepository.findByLoginId("manager01")).willReturn(Optional.of(member));

        CustomUserDetails result = (CustomUserDetails) customUserDetailsService.loadUserByUsername("manager01");

        assertThat(result.getUsername()).isEqualTo("manager01");
        assertThat(result.getName()).isEqualTo("김매니저");
        assertThat(result.getRole()).isEqualTo(Role.MANAGER);
        assertThat(result.isEnabled()).isTrue();
    }
}
