package modackbulz.app.Application.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
   * Elasticsearch 'camp_final' 인덱스의 문서를 매핑하기 위한 마스터 엔티티 클래스.
   * @JsonIgnoreProperties(ignoreUnknown = true) 어노테이션 덕분에,
   * ES에만 있고 이 클래스에는 없는 필드가 있어도 에러 없이 안전하게 파싱됩니다.
   */
  @Getter
  @Setter
  @NoArgsConstructor
  @ToString
  @JsonIgnoreProperties(ignoreUnknown = true)
  public class CampNm {

    // 검색 결과에 공통적으로 필요한 필드
    private Long contentId;
    private String facltNm;
    private String doNm;
    private String firstImageUrl;
    private String addr1;
    private String lineIntro;

    // 테마 자동완성 및 검색을 위한 필드
    private List<String> themaEnvrnCl;
    private String column2;

    // 통합 검색을 위한 필드 (이 필드는 자바 파이프라인에서 채워짐)
    private String keyword_all;
    private List<String> keyword_theme;

  }
