package modackbulz.app.Application.web.form.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditForm {
  private String id;        // readonly

  @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
  @Size(max = 12, message = "닉네임은 최대 12자까지 입력 가능합니다.")
  @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다.")
  private String nickname;

  @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
  @Size(max = 11, message = "전화번호는 최대 11자리 숫자여야 합니다.")
  @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 숫자만 입력해야 하며, 10~11자리여야 합니다.")
  private String tel;

  private String region;

  @NotBlank(message = "이메일은 필수 입력 항목입니다.")
  @Email(message = "올바른 이메일 형식이 아닙니다.")
  @Size(max = 30, message = "이메일은 최대 30자까지 입력 가능합니다.")
  private String email;

  private String authCode; // 인증번호 필드 추가
}