package modackbulz.app.Application.web.form.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginMember {
  private Long memberId;     // 관리 번호
  private String id;         // 사용자 로그인 ID
  private String email;
  private String nickname;
  private String gubun;      // 회원 구분: 관리자/일반회원 구분을 위함
}