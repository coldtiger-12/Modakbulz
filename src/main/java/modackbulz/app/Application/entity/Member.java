package modackbulz.app.Application.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
  private String isDel;          // IS_DEL 탈퇴 여부 ('Y' / 'N')
  private LocalDateTime delDate; // DEL_DATE 삭제 예정 시각
}
