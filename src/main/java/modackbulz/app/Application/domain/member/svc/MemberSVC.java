package modackbulz.app.Application.domain.member.svc;

import modackbulz.app.Application.entity.Member;
import java.util.Optional;

public interface MemberSVC {

  // 회원 가입
  Member insertMember(Member member);

  // 회원 존재 유무 확인(아이디)
  boolean isExist(String id);

  // 회원 존재 유무 확인(닉네임) (추가)
  boolean isExistNickname(String nickname);

  // 회원 조회(멤버 아이디)
  Optional<Member> findByMemeberId(Long memberId);

  // 회원 조회(아이디)
  Optional<Member> findById(String id);

  // 비밀번호 변경
  boolean changePassword(Long memberId, String newPwd);

  // 탈퇴 처리 요청
  boolean requestDeletion(Long memberId);

  // [추가] 탈퇴 취소 서비스 인터페이스
  boolean cancelDeletion(Long memberId);

  // 회원 정보 수정
  boolean updateMember(Member member);

  //이메일로 회원 아이디 찾기
  Optional<String> findIdByEmail(String email);

  // 임시 비밀번호 발급 및 이메일 전송
  boolean issueTempPassword(String id, String email);
}