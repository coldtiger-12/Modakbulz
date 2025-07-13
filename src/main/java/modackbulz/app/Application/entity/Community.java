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
public class Community {
  private Long coId;
  private Long memberId;
  private String title;
  private String writer;
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private int viewC;

//  // 조회수 getter (null-safe)
//  public int getViewCountAsInt() {
//    try {
//      return Integer.parseInt(Optional.ofNullable(viewC).orElse("0"));
//    } catch (NumberFormatException e) {
//      return 0;
//    }
//  }
//
//  // 조회수 증가
//  public void increaseViewCount() {
//    int newCount = getViewCountAsInt() + 1;
//    this.viewC = String.valueOf(newCount);
//  }
}