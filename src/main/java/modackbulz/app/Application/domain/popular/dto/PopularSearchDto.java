package modackbulz.app.Application.domain.popular.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularSearchDto {
  private String keyword; // 인기 검색어
  private long count;     // 검색 횟수
}