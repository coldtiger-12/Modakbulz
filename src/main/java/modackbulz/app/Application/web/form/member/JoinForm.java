package modackbulz.app.Application.web.form.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.AssertTrue;

@Data
public class JoinForm {

  @NotBlank(message = "아이디는 필수 입력 항목입니다.")
  @Size(max = 10, message = "아이디는 최대 10자까지 입력 가능합니다.")
  @Pattern(regexp = "^[A-Za-z0-9]+$", message = "아이디는 영문 대소문자와 숫자만 사용할 수 있습니다.")
  private String id;

  @NotBlank(message = "이메일은 필수 입력 항목입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  @Size(max = 30, message = "이메일은 최대 30자까지 입력 가능합니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
  @Size(min = 8, max = 13, message = "비밀번호는 8~13자 사이여야 합니다.")
  @Pattern(
      regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+=-]).{8,}$",
      message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 포함해야 합니다."
  )
  private String pwd;

  @NotBlank(message = "비밀번호 확인은 필수입니다.")
  private String pwdChk;

  @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
  @Size(max = 12, message = "닉네임은 최대 12자까지 입력 가능합니다.")
  @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
  private String nickname;

  @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
  @Size(max = 11, message = "전화번호는 최대 11자리 숫자여야 합니다.")
  @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자만 입력해야 하며, 10~11자리여야 합니다.")
  private String tel;

  private String gender;  // '남' 또는 '여' 선택. 선택사항

  private String region;  // 선택사항 (SQL상 NULL 허용)

  // 비밀번호 일치 여부 확인
  @AssertTrue(message = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")
  public boolean isPasswordMatching() {
    if (pwd == null || pwdChk == null) return false;
    return pwd.equals(pwdChk);
  }
}