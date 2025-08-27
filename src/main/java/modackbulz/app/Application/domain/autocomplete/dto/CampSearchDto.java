package modackbulz.app.Application.domain.autocomplete.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)

public class CampSearchDto {

  // 검색 결과에 공통적으로 필요한 필드
  private Long contentId;
  private String facltNm;
  private String doNm;
  private String firstImageUrl;
  private String addr1;
  private String lineIntro;

  // 테마 자동완성 및 검색을 위한 필드
  private List<String> themaEnvrnCl;
  private List<String> column2;

  // 통합 검색을 위한 필드 (이 필드는 자바 파이프라인에서 채워짐)
  private String keyword_all;
  private List<String> keyword_theme;

}
