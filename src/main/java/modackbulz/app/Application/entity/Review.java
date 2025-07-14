package modackbulz.app.Application.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Review {
  private Long revId;         // REV_ID
  private Long contentId;     // CONTENT_ID
  private Long memberId;      // MEMBER_ID
  private String writer;      // WRITER
  private String content;     // CONTENT
  private LocalDateTime createdAt; // CREATED_AT
  private LocalDateTime updatedAt; // UPDATED_AT
  private int score;          // SCORE
  private String keywordSummary;
  private String photoUrl;
}