package com.stockandorder.global.auth;

import com.stockandorder.domain.member.entity.Member;
import com.stockandorder.domain.member.enums.Role;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security의 UserDetails를 구현한 인증 주체.
 *
 * [Member 엔티티를 참조하지 않고 값만 복사해 보관하는 이유]
 * 이 객체는 세션에 저장된다. 세션 저장소를 Redis로 분리하면서 세션은 더 이상 JVM 안의 객체 참조가
 * 아니라 직렬화되어 외부로 나가는 바이트가 되었다. UserDetails가 Serializable을 상속하므로 선언상으로는
 * 직렬화 대상이었지만, 직렬화되지 않는 Member 엔티티를 필드로 들고 있어 실제로는 저장 시점에 깨진다.
 * 엔티티를 그대로 담을 경우의 문제는 세 가지다.
 * 1) 엔티티에 필드가 추가/변경되면 이미 저장된 세션이 역직렬화에 실패한다. 배포 때마다 전원
 *    로그아웃된다면 세션을 외부 저장소로 분리한 이유 자체가 사라진다.
 * 2) 지연 로딩 프록시나 연관 엔티티가 직렬화 그래프에 함께 딸려 들어갈 수 있다.
 * 3) 인증에 필요한 값은 몇 개뿐인데 엔티티 전체를 매 요청마다 왕복시킨다.
 * 따라서 인증 판단에 필요한 최소 값만 복사해 스냅샷으로 보관한다.
 *
 * [권한은 로그인 시점의 스냅샷이라는 점]
 * DB에서 Role을 바꿔도 이미 발급된 세션에는 이전 Role이 남는다. 즉시 반영이 필요하면
 * 해당 사용자의 세션을 저장소에서 제거해 재인증을 유도한다.
 */
public class CustomUserDetails implements UserDetails, CredentialsContainer {

    private static final long serialVersionUID = 1L;

    private final Long memberId;
    private final String loginId;
    private final String name;
    private final Role role;
    private final boolean active;

    /** 인증 완료 후 eraseCredentials()로 지워지므로 final이 아니다. */
    private String password;

    public CustomUserDetails(Member member) {
        this.memberId = member.getMemberId();
        this.loginId = member.getLoginId();
        this.name = member.getName();
        this.role = member.getRole();
        this.active = member.isActive();
        this.password = member.getPassword();
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** Spring Security에서 로그인 식별자로 사용하는 값 (loginId) */
    @Override
    public String getUsername() {
        return loginId;
    }

    /**
     * 인증이 끝나면 Spring Security가 호출해 비밀번호 해시를 지운다.
     * 세션이 외부 저장소에 기록되는 이상, 인증 이후로는 쓰지 않는 해시를 남겨둘 이유가 없다.
     */
    @Override
    public void eraseCredentials() {
        this.password = null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** isActive = false 이면 Spring Security가 DisabledException 발생 → 로그인 실패 처리 */
    @Override
    public boolean isEnabled() {
        return active;
    }
}
