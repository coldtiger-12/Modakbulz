package modackbulz.app.Application.domain.member.dao;

import modackbulz.app.Application.entity.Member;
import java.util.Optional;

public interface MemberDAO {

  /**
   * 회원 가입
   * @param member 회원정보
   * @return 가입후 정보
   */
  Member insertMember(Member member);

  /**
   * 아이디로 회원 유무 확인
   * @param id
   * @return
   */
  boolean isExist(String id);

  /**
   * 닉네임으로 회원 유무 확인 (추가)
   * @param nickname
   * @return
   */
  boolean isExistNickname(String nickname);

  /**
   * 회원 조회
   * @param memberId
   * @return 회원정보
   */
  Optional<Member> findByMemeberId(Long memberId);

  /**
   * 회원 조회
   * @param id
   * @return 회원 정보
   */
  Optional<Member> findById(String id);

  /**
   * 비밀번호 변경
   * @param memberId
   * @param newPwd
   * @return 변경 성공 여부
   */
  boolean changePassword(Long memberId, String newPwd);

  /**
   * 탈퇴 처리 (IS_DEL = 'Y')
   * @param memberId
   * @return 변경 성공 여부
   */
  boolean deleteMember(Long memberId);

  /**
   * 회원 정보 수정
   * @param member 수정할 회원 정보 (memberId 필수)
   * @return 수정 성공 여부
   */
  boolean updateMember(Member member);
}