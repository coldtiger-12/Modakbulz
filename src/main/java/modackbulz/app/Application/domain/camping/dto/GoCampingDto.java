package modackbulz.app.Application.domain.camping.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j; // Slf4j import 추가

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 응답의 모든 필드를 매핑하지 않아도 오류가 나지 않도록 설정
public class GoCampingDto {
  private Response response;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Response {
    private Body body;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Body {
    private Items items;
    private int totalCount;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Items {
    private List<Item> item;
  }

  @Slf4j // 로그 사용을 위한 어노테이션 추가
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Item {
    private Long contentId;
    private String facltNm; // 야영장명
    private String lineIntro; // 한줄소개
    private String intro;   //캠핑장소개
    private String addr1; // 주소
    private String firstImageUrl; // 대표 이미지
    private String tel; // 전화번호
    private String homepage; // 홈페이지
    private String resveUrl; // 예약 페이지
    private String sbrsCl; // 부대시설
    private String themaEnvrnCl; // 테마환경
    private Double mapX; // 경도
    private Double mapY; // 위도
    private String featureNm; // 특징
    private String induty; // 업종 (일반야영장, 자동차야영장 등)
    private String lctCl; // 입지구분 (해변, 섬, 산, 숲, 도심 등)
    private String operPdCl; // 운영기간 (봄, 여름, 가을, 겨울)
    private Integer gnrlSiteCo; // 주요시설 일반야영장 사이트 수
    private Integer autoSiteCo; // 주요시설 자동차야영장 사이트 수
    private Integer glampSiteCo; // 주요시설 글램핑 사이트 수
    private Integer caravSiteCo; // 주요시설 카라반 사이트 수
    
    // API가 보내주는 String 값을 숫자 타입으로 변환하기 위한 Setter들
    public void setContentId(String contentId) {
      this.contentId = safeParseLong(contentId);
    }
    public void setGnrlSiteCo(String gnrlSiteCo) {
      this.gnrlSiteCo = safeParseInt(gnrlSiteCo);
    }
    public void setAutoSiteCo(String autoSiteCo) {
      this.autoSiteCo = safeParseInt(autoSiteCo);
    }
    public void setGlampSiteCo(String glampSiteCo) {
      this.glampSiteCo = safeParseInt(glampSiteCo);
    }
    public void setCaravSiteCo(String caravSiteCo) {
      this.caravSiteCo = safeParseInt(caravSiteCo);
    }
    public void setMapX(String mapX) {
      this.mapX = safeParseDouble(mapX);
    }
    public void setMapY(String mapY) {
      this.mapY = safeParseDouble(mapY);
    }

    // --- 타입 변환 헬퍼 메서드 ---
    private Long safeParseLong(String s) {
      if (s == null || s.isBlank()) return null;
      try {
        return Long.parseLong(s);
      } catch (NumberFormatException e) {
        log.warn("Long 타입 변환 실패: '{}'", s);
        return null;
      }
    }
    private Integer safeParseInt(String s) {
      if (s == null || s.isBlank()) return 0;
      try {
        return Integer.parseInt(s);
      } catch (NumberFormatException e) {
        log.warn("Integer 타입 변환 실패: '{}'", s);
        return 0;
      }
    }
    private Double safeParseDouble(String s) {
      if (s == null || s.isBlank()) return null;
      try {
        return Double.parseDouble(s);
      } catch (NumberFormatException e) {
        log.warn("Double 타입 변환 실패: '{}'", s);
        return null;
      }
    }
  }
}