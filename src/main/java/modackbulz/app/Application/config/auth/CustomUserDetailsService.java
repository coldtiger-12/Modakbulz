package modackbulz.app.Application.config.auth;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.member.dao.MemberDAO;
import modackbulz.app.Application.entity.Member;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final MemberDAO memberDAO;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // memberDAO를 통해 DB에서 사용자 정보를 조회합니다.
    Optional<Member> memberOptional = memberDAO.findById(username);
    // 사용자가 존재하지 않으면 예외를 발생시킵니다.
    Member member = memberOptional.orElseThrow(() ->
        new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

    // 조회된 Member 객체를 CustomUserDetails로 감싸서 반환합니다.
    return new CustomUserDetails(member);

  }
}
