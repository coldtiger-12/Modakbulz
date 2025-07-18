package modackbulz.app.Application.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampScrap {
  private Long scrapId;
  private Long memberId;
  private Long contentId;
  private String facltNm;
  private String firstImageUrl;
  private String addr1;
  private LocalDateTime createdAt;
}