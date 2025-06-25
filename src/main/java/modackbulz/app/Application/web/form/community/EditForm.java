package modackbulz.app.Application.web.form.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditForm {
  private Long coId;

  @NotBlank(message = "제목은 필수 입력 항목입니다.")
  @Size(max = 50, message = "제목은 최대 50자까지 입력 가능합니다.")
  private String title;
  private String writer;

  @NotBlank(message = "내용은 필수 입력 항목입니다.")
  @Size(max = 1000, message = "내용은 최대 1000자까지 입력 가능합니다.")
  private String content;
}