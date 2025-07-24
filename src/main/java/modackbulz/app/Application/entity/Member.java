package modackbulz.app.Application.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
  private Long memberId;         // MEMBER_ID 내부관리번호
  private String gubun;          // GUBUN 회원 구분 ('A' = 관리자 등)
  private String id;             // ID 사용자 아이디
  private String pwd;            // PWD 비밀번호
  private String email;          // EMAIL 이메일
  private String tel;            // TEL 전화번호
  private String nickname;       // NICKNAME 별명
  private String gender;         // GENDER 성별 (남, 여)
  private String region;         // REGION 지역

  // [수정] isDel 필드를 status 필드로 변경
  // DB의 IS_DEL 컬럼과 매핑됨 'N' 대신 'ACTIVE', 'Y' 대신 'PENDING_DELETION'을 사용
  private MemberStatus status;

  // [수정] delDate 필드의 이름을 명확하게 변경
  // DB의 DEL_DATE 컬럼과 매핑됨
  private LocalDateTime deletionRequestedAt;

  // Lombok의 @Builder가 생성자 기반으로 동작하므로,
  // 기본 상태를 ACTIVE로 설정하기 위해 아래와 같은 메소드를 추가 가능
  public static class MemberBuilder{
    private MemberStatus status = MemberStatus.ACTIVE;
  }
}