package modackbulz.app.Application.domain.autocomplete.svc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.autocomplete.dao.SearchDAO;
import modackbulz.app.Application.domain.autocomplete.dto.AutocompleteDto;
import modackbulz.app.Application.domain.autocomplete.dto.CampSearchDto;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSVCImpl implements SearchSVC {

  private final SearchDAO searchDAO;

  @Override
  public List<AutocompleteDto.RegionResponse> getAutocompleteRegion(String keyword){
    try{
      return searchDAO.searchCampsitesByRegion(keyword).hits().hits().stream()
        .map(hit -> {
          CampSearchDto campSearchDto = hit.source();

          // 가져온 데이터가 어떤 모양인지 확인 하기 위한 로그 ( 디버깅에 사용 )
          log.info("변환된 source 객체 : {}", campSearchDto);

          // review가 null일 경우를 대비한 방어코드
          if (campSearchDto == null){
            return null;
          }
          return new AutocompleteDto.RegionResponse(campSearchDto.getDoNm(), campSearchDto.getFacltNm());
        })
        .filter(dto -> dto != null) // null인 경우 제외
        .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("지역 자동완성 검색에 실패했습니다.", e);
    }
  }

  @Override
  public List<AutocompleteDto.CampResponse> getAutocompleteCampsites(String keyword) {
    try {
      return searchDAO.searchCampsitesByName(keyword).hits().hits().stream()
          .map(hit -> hit.source().getFacltNm())
          .collect(Collectors.toSet())
          .stream()
          .map(AutocompleteDto.CampResponse::new)
          .limit(10)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("캠핑장 자동완성 검색에 실패했습니다.", e);
    }
  }

  @Override
  public List<AutocompleteDto.KeywordResponse> getAutocompleteKeywords(String keyword) {
    try {
      var aggregations = searchDAO.aggregateKeywords(keyword).aggregations();

      Stream<String> officialThemes = aggregations.get("unique_official_themes").sterms().buckets().array().stream()
          .map(bucket -> bucket.key().stringValue());

      Stream<String> naverKeywords = aggregations.get("unique_keyword_themes").sterms().buckets().array().stream()
          .map(bucket -> bucket.key().stringValue());

      return Stream.concat(officialThemes, naverKeywords)
          .collect(Collectors.toSet())
          .stream()
          .filter(item -> item.contains(keyword))
          .map(AutocompleteDto.KeywordResponse::new)
          .limit(12)
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new RuntimeException("키워드 자동완성 검색에 실패했습니다.", e);
    }
  }

  @Override
  public List<String> getNaverKeywordForCamp(Long contentId){
    try{
      CampSearchDto camp = searchDAO.findCampById(contentId.toString());

      if (camp == null){
        return Collections.emptyList();
      }

      // 두개의 키워드 리스트 각각 준비
      List<String> officialThemes = camp.getThemaEnvrnCl() != null ? camp.getThemaEnvrnCl() : Collections.emptyList();
      List<String> keywords = camp.getColumn2() != null ? camp.getColumn2() : Collections.emptyList();

      return Stream.concat(officialThemes.stream(), keywords.stream())
          .filter(Objects :: nonNull) // null 값 제거
          .map(keyword -> keyword.trim().replaceAll("\\s*",""))   // 앞뒤 공백 표준화
          .filter(keyword -> !keyword.isBlank())    // 표준화 이후 빈 문자열이 된 경우 제거
          .collect(Collectors.toSet())  // 중복 제거
          .stream()
          .collect(Collectors.toList());

    } catch (IOException e){
      System.err.println("ID" + contentId + "캠핑장의 키워드를 가져오는데에 실패했습니다: " +  e.getMessage());
    }
    // 실패하거나 키워드가 없을시 빈 리스트 반환
    return Collections.emptyList();
  }
}