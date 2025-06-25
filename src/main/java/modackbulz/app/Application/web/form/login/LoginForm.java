package modackbulz.app.Application.web.form.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginForm {

  @NotBlank(message = "아이디는 필수 입력 항목입니다.")
  @Size(max = 10, message = "아이디는 최대 10자까지 입력 가능합니다.")
  private String id;

  @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
  @Size(min = 8, max = 13, message = "비밀번호는 8~13자 사이여야 합니다.")
  private String pwd;

}