package modackbulz.app.Application.web;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.autocomplete.dto.AutocompleteDto;
import modackbulz.app.Application.domain.autocomplete.svc.SearchSVC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchApiController {

  private final ElasticsearchClient elasticsearchClient;

  private final SearchSVC searchSVC;

  // 로그를 찍기 위한 로거 추가
  private static final Logger log = LoggerFactory.getLogger(SearchApiController.class);

  @GetMapping("/autocomplete/region")
  public ResponseEntity<List<AutocompleteDto.RegionResponse>> autocompleteRegion(
      @RequestParam("keyword") String keyword) throws IOException{

    List<AutocompleteDto.RegionResponse> suggestions = searchSVC.getAutocompleteRegion(keyword);

    log.info("자동완성 캠핑장 + 지역 목록 : {}", suggestions);

    return ResponseEntity.ok(suggestions);

  }

  @GetMapping("/autocomplete/facltNm")
  public ResponseEntity<List<AutocompleteDto.CampResponse>> autocompleteCampsite(
      @RequestParam("keyword") String keyword) throws IOException {

    List<AutocompleteDto.CampResponse> suggestions = searchSVC.getAutocompleteCampsites(keyword);

    log.info("자동완성 캠핑장 목록 : {}", suggestions);

    return ResponseEntity.ok(suggestions);
  }

  @GetMapping("/autocomplete/keyword")
  public ResponseEntity<List<AutocompleteDto.KeywordResponse>> autocompleteKeyword(
      @RequestParam("keyword") String keyword) throws IOException{

    List<AutocompleteDto.KeywordResponse> suggestions = searchSVC.getAutocompleteKeywords(keyword);

    log.info("자동완성 키워드 목록: {}", suggestions);

    return ResponseEntity.ok(suggestions);

  }

}
