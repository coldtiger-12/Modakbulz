package modackbulz.app.Application.config.auth;

import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.entity.MemberStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security가 사용하는 UserDetails 인터페이스의 구현체입니다.
 * 이 클래스는 Member 엔티티 객체를 감싸서 Spring Security가 인증 및 권한 부여에
 * 필요한 정보를 제공하는 역할을 합니다.
 */

@Slf4j
public class CustomUserDetails implements UserDetails {

  private final Member member; // 데이터베이스에서 조회해 온 실제 회원 정보

  public CustomUserDetails(Member member) {
    this.member = member;
  }

  public Member getMember() {
    return member;
  }

  // memberId를 쉽게 가져올 수 있도록 메서드 추가
  public Long getMemberId() {
    return member.getMemberId();
  }

  // 사용자 닉네임을 반환합니다
  public String getNickname() {
    return member.getNickname();
  }

  // 사용자의 회원등급을 반환합니다.
  public String getGubun() {
    return member.getGubun();
  }

  // 사용자 이메일 반환
  public String getEmail() {
    return member.getEmail();
  }

  // 1. 사용자의 '권한' 목록을 반환합니다.
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // Member 객체의 gubun 필드('U' 또는 'A')를 기반으로 권한을 생성합니다.
    // Spring Security는 권한 앞에 "ROLE_" 접두사가 붙는 것을 기본으로 합니다.
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getGubun()));
  }

  // 2. 사용자의 '아이디'를 반환합니다.
  @Override
  public String getUsername() {
    return member.getId();
  }

  // 3. 사용자의 '비밀번호'를 반환합니다. (암호화된 값)
  @Override
  public String getPassword() {
    log.info(">>>> getPassword() 호출됨. DB에서 가져온 암호화된 비밀번호: '{}'", member.getPwd());
    return member.getPwd();
  }

  // 4. 계정이 만료되었는지 여부를 반환합니다. (true: 만료되지 않음)
  @Override
  public boolean isAccountNonExpired() {
    return true; // 지금은 사용하지 않으므로 true로 설정
  }

  // 5. 계정이 잠겨있는지 여부를 반환합니다. (true: 잠기지 않음)
  @Override
  public boolean isAccountNonLocked() {
    return true; // 지금은 사용하지 않으므로 true로 설정
  }

  // 6. 비밀번호가 만료되었는지 여부를 반환합니다. (true: 만료되지 않음)
  @Override
  public boolean isCredentialsNonExpired() {
    return true; // 지금은 사용하지 않으므로 true로 설정
  }

  // 7. 계정이 활성화 상태인지 여부를 반환합니다. (true: 활성화)
  @Override
  public boolean isEnabled() {
    // 회원의 상태가 ACTIVE일 때만 true(활성화)를 반환합니다.
    return this.member.getStatus() == MemberStatus.ACTIVE;
  }

}

// isDel 필드가 'ACTIVE'일 때만 계정을 활성화 상태(true)로 간주합니다.
//    return "ACTIVE".equals(member.getStatus());


//public boolean isEnabled() {
//// ⭐️ 중요: 로그인 자체는 성공시켜야 하므로, 무조건 true를 반환합니다.
//// 실제 계정 상태(활성/탈퇴요청)에 따른 분기 처리는 CustomLoginSuccessHandler에서 담당합니다.
//  log.info(">>>> isEnabled() 호출됨. 사용자 '{}'의 계정은 일단 활성(true)으로 간주합니다.", this.member.getId());
//  return true;
//
//}

//public boolean isEnabled() {
//  boolean enabled = this.member.getStatus() == MemberStatus.ACTIVE;
//  log.info(">>>> isEnabled() 호출됨. 사용자 '{}'의 상태는 '{}' 이므로, enabled = {}",
//      this.member.getId(), this.member.getStatus(), enabled);
//  return enabled;
//}
