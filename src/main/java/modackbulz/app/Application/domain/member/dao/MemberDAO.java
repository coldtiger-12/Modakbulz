package modackbulz.app.Application.domain.member.dao;

import modackbulz.app.Application.entity.Member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberDAO {

  /**
   * 회원 가입
   * @param member 회원정보
   * @return 가입후 정보
   */
  Member insertMember(Member member);

  /**
   * 모든 회원 정보 조회
   * @return 모든 회원 정보
   */
  List<Member> findAll();

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

//  // 회원 탈퇴 메서드 (임시)
//  boolean deleteMember(Long memberId);

  /**
   * 회원 탈퇴 요청
   * @param memberId
   * @return 변경 성공 여부
   */
  boolean requestDeletion(Long memberId);

  /**
   * 회원 탈퇴 취소
   * @param memberId
   * @return 탈퇴 취소 성공 여부
   */
  boolean cancelDeletion(Long memberId);

  // 스케쥴러가 사용할 메서드
  List<Member> findMembersForHard(LocalDateTime criteriaDate);

  // 스케쥴러가 사용할 실제 삭제 메서드
  void hardDeleteMembers(List<Long> memberIds);

  /**
   * 회원 정보 수정
   * @param member 수정할 회원 정보 (memberId 필수)
   * @return 수정 성공 여부
   */
  boolean updateMember(Member member);
}