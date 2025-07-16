package modackbulz.app.Application.web.form.review;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class ReviewForm {

    private Long reviewId; // 수정 시 사용

    @NotNull(message = "캠핑장 ID는 필수입니다.")
    private Long campingId;

    @NotBlank(message = "리뷰 내용은 비워둘 수 없습니다.")
    @Size(min = 10, max = 2000, message = "리뷰는 최소 10자, 최대 2000자까지 작성 가능합니다.")
    private String content;

    @NotNull(message = "평점을 선택해주세요.")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다.")
    private Integer score;

    // 특징 선택 (야놀자 스타일)
    private String[] features;

    // 추가된 필드
    private List<Long> keywordIds;
    private List<MultipartFile> imageFiles;
} 