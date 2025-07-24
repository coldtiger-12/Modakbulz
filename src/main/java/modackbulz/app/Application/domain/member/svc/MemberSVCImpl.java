package modackbulz.app.Application.domain.member.svc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.dao.MemberDAO;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.global.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder; // PasswordEncoder import
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSVCImpl implements MemberSVC {

  private final MemberDAO memberDAO;
  private final PasswordEncoder passwordEncoder; // PasswordEncoder 주입
  private final EmailService emailService;

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

  /**
   * 이메일로 회원 아이디 찾기
   * @param email
   * @return 아이디 Optional 객체
   */
  @Override
  public Optional<String> findIdByEmail(String email){
    List<Member> allMembers = memberDAO.findAll();
    return allMembers.stream()
        .filter(member -> email.equals(member.getEmail()))  // 복호화된 이베일과 사용자 입력 이메일 비교
        .map(Member::getId)
        .findFirst();
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
  public boolean issueTempPassword(String id, String email){
    Optional<Member> memberOpt = memberDAO.findById(id);

    // 아이디 존재 여부 및 이메일 일치 여부 확인
    if (memberOpt.isEmpty() || !email.equals(memberOpt.get().getEmail())){
      return false;
    }

    Member member = memberOpt.get();

    // 임시 비밀번호 생성
    String tempPassword = createTempPassword();

    // 이메일 발송
    try{
      String subject = "[모닥불즈] 임시 비밀번호 안내입니다.";
      String text = "회원님의 임시 비밀번호는 [ " + tempPassword + " ] 입니다. 로그인 후 반드시 비밀번호를 변경해주세요.";
      emailService.sendEmail(member.getEmail(), subject, text);
    } catch (Exception e) {
      log.error("임시 비밀번호 이메일 발송 실패", e);
      return false; // 이메일 발송 실패 시 작업 중단
    }

    // 생성된 임시 비밀번호를 암호화하여 DB에 업데이트
    String encryptedPassword = passwordEncoder.encode(tempPassword);
    return memberDAO.changePassword(member.getMemberId(), encryptedPassword);
  }

  // 임시 비밀번호 생성 로직 (8자리 영문 + 숫자)
  private String createTempPassword(){
    Random random = new Random();
    return random.ints(48,123)
        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
        .limit(8)
        .collect(StringBuffer::new, StringBuffer::appendCodePoint, StringBuffer::append)
        .toString();

  }

  // [수정] deleteMember를 requestDeletion으로 변경하고 DAO 호출
  @Override
  public boolean requestDeletion(Long memberId){
    return memberDAO.requestDeletion(memberId);
  }

  // [추가] 탈퇴 취소 서비스 메소드
  @Override
  public boolean cancelDeletion(Long memberId){
    return memberDAO.cancelDeletion(memberId);
  }

  @Override
  public boolean updateMember(Member member) {
    return memberDAO.updateMember(member);
  }
}
