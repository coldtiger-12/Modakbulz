package modackbulz.app.Application.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faq {
  private Long faqId;
  private Long memberId;
  private String writer;
  private String content;
  private LocalDateTime createdAt;
}
