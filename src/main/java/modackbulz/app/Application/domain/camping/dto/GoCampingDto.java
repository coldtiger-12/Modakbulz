package modackbulz.app.Application.domain.camping.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
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

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Item {
    private String contentId;
    private String facltNm; // 야영장명
    private String lineIntro; // 한줄소개
    private String addr1; // 주소
    private String firstImageUrl; // 대표 이미지
    private String tel; // 전화번호
    private String homepage; // 홈페이지
    private String resveUrl; // 예약 페이지
    private String sbrsCl; // 부대시설
    private String themaEnvrnCl; // 테마환경
  }
}