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

  // 탈퇴 처리 (IS_DEL = 'Y')
  boolean deleteMember(Long memberId);

  // 회원 정보 수정
  boolean updateMember(Member member);
}