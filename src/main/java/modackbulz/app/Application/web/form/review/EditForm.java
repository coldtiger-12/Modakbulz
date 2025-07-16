package modackbulz.app.Application.web.form.review;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class EditForm {
  @NotBlank(message = "리뷰 내용은 비워둘 수 없습니다.")
  @Size(max = 500, message = "리뷰는 최대 500자까지 작성 가능합니다.")
  private String content;

  @NotNull(message = "평점을 선택해주세요.")
  @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
  @Max(value = 5, message = "평점은 5점 이하이어야 합니다.")
  private Integer score;

  // 1. 수정할 키워드 ID 목록
  private List<Long> keywordIds;

  // 2. 새로 추가할 이미지 파일 목록
  private List<MultipartFile> newImageFiles;

  // 3. 삭제할 기존 이미지의 파일 ID 목록
  private List<Long> deletedFileIds;

  // 취소를 눌렀을때 상세 페이지로 돌아갈 수 있도록 하기 위함
  private Long contentId;
}