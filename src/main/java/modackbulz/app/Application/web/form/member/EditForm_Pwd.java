package modackbulz.app.Application.web.form.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditForm_Pwd {
  @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
  @Size(min = 8, max = 13, message = "비밀번호는 8~13자 사이여야 합니다.")
  @Pattern(
      regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$",
      message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 포함해야 합니다."
  )
  private String pwd;

  @NotBlank(message = "비밀번호 확인은 필수 입력 항목입니다.")
  private String pwdCheck;
}