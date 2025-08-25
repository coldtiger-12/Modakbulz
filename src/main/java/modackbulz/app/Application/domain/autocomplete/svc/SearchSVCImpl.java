package modackbulz.app.Application.domain.autocomplete.svc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.autocomplete.dao.SearchDAO;
import modackbulz.app.Application.domain.autocomplete.dto.AutocompleteDto;
import modackbulz.app.Application.entity.CampNm;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
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
          CampNm campNm = hit.source();

          // 가져온 데이터가 어떤 모양인지 확인 하기 위한 로그 ( 디버깅에 사용 )
          log.info("변환된 source 객체 : {}", campNm);

          // review가 null일 경우를 대비한 방어코드
          if (campNm == null){
            return null;
          }
          return new AutocompleteDto.RegionResponse(campNm.getDoNm(), campNm.getFacltNm());
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
}