package modackbulz.app.Application.domain.member.svc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.dao.MemberDAO;
import modackbulz.app.Application.entity.Member;
import org.springframework.security.crypto.password.PasswordEncoder; // PasswordEncoder import
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSVCImpl implements MemberSVC {

  private final MemberDAO memberDAO;
  private final PasswordEncoder passwordEncoder; // PasswordEncoder 주입

  /**
   * [중요] 회원 가입 시 비밀번호를 암호화하여 저장합니다.
   */
  @Override
  public Member insertMember(Member member) {
    // 사용자가 입력한 비밀번호를 암호화합니다.
    String encryptedPassword = passwordEncoder.encode(member.getPwd());
    member.setPwd(encryptedPassword);

    return memberDAO.insertMember(member);
  }

  @Override
  public boolean isExist(String id) {
    return memberDAO.isExist(id);
  }

  @Override
  public boolean isExistNickname(String nickname) {
    return memberDAO.isExistNickname(nickname);
  }

  @Override
  public Optional<Member> findByMemeberId(Long memberId) {
    return memberDAO.findByMemeberId(memberId);
  }

  @Override
  public Optional<Member> findById(String id) {
    return memberDAO.findById(id);
  }

  @Override
  public boolean changePassword(Long memberId, String newPwd) {
    // MyPageController에서 이미 암호화된 비밀번호를 전달하므로 DAO를 그대로 호출합니다.
    return memberDAO.changePassword(memberId, newPwd);
  }

  @Override
  public boolean deleteMember(Long memberId) {
    return memberDAO.deleteMember(memberId);
  }

  @Override
  public boolean updateMember(Member member) {
    return memberDAO.updateMember(member);
  }
}
