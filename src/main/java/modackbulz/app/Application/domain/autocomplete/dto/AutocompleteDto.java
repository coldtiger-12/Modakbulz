package modackbulz.app.Application.domain.autocomplete.dto;

public class AutocompleteDto {

  // 지역 + 캠핑장 이름 자동완성용 - 지역기반
  public record RegionResponse(String doNm, String facltNm) {}

  // 캠핑장 이름 자동완성용
  public record CampResponse(String facltNm) {}

  // 키워드 자동완성용
  public record KeywordResponse(String keyword) {}


}