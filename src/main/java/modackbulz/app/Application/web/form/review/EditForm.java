package modackbulz.app.Application.web.form.review;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EditForm {
  @NotBlank(message = "리뷰 내용은 비워둘 수 없습니다.")
  @Size(max = 500, message = "리뷰는 최대 500자까지 작성 가능합니다.")
  private String content;

  @NotNull(message = "평점을 선택해주세요.")
  @Min(value = 0, message = "평점은 0점 이상이어야 합니다.")
  @Max(value = 5, message = "평점은 5점 이하이어야 합니다.")
  private Integer score;

  // 취소를 눌렀을때 상세 페이지로 돌아갈 수 있도록 하기 위함
  private Long contentId;
}