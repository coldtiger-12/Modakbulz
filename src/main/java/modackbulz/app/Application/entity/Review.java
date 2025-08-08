package modackbulz.app.Application.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Review {
  private Long revId;         // REV_ID
  private Long contentId;     // CONTENT_ID
  private Long memberId;      // MEMBER_ID
  private String writer;      // WRITER
  private String content;     // CONTENT
  private LocalDateTime createdAt; // CREATED_AT
  private LocalDateTime updatedAt; // UPDATED_AT
  private int score;          // SCORE
  // 키워드 ID 목록 (다중 선택)
  private List<Long> keywordIds;

  // 업로드된 파일 ID 목록 또는 별도 FileDTO 사용 가능
  private List<UploadFile> files;
}