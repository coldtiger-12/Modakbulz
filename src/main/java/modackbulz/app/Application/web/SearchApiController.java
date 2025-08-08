package modackbulz.app.Application.web;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchApiController {

  private final ElasticsearchClient esClient;

  // 로그를 찍기 위한 로거 추가
  private static final Logger log = LoggerFactory.getLogger(SearchApiController.class);

  // 간단한 자동완성 결과 DTO (캠핑장명 + 지역)
  private record AutocompleteDTO(String facltNm, String doNm){}

  // 지역 자동 완성만을 위한 DTO
  private record RegionAutocompleteDTO(String doNm){}


  @GetMapping("/autocomplete")
  public ResponseEntity<List<RegionAutocompleteDTO>> autocompleteRegion(
      @RequestParam("keyword") String keyword) throws IOException{

    // 집계 이름을 "unigue_regions"로 명확하게 변경
    String aggregationName = "unique-regions";

    // 검색(query)이 아닌 집계(aggregations)를 사용하도록 쿼리를 완전 수정
    SearchResponse<Void> response = esClient.search(s -> s
        .index("camp_3")   // 검색할 인덱스 이름 지정
        .aggregations(aggregationName,a -> a   // unique-regions 이라는 이름으로 집계 시작
            .terms(t -> t    // terms 집계를 사용 (중복 없는 목록 가져오기 위함)
                .field("doNm.keyword")  // 중복을 제거할 필드 (index 안에 doNm.keyword 필드가 존재하지 않아 text 필드 doNm 그대로 사용)
                .size(17)      // 최대 결과 갯수 지정
                // include 대신, wildcard 쿼리를 집계의 필터로 사용하는 것이 더 안정적임
                // 키워드 필터링을 위해서 다른 방식을 사용 함
            )
        ),
        Void.class
    );

    // 집계 결과에서 데이터를 꺼내 DTO 리스트로 변환
    List<RegionAutocompleteDTO> suggestions = response.aggregations()
        .get(aggregationName)// 수정된 집계 이름으로 가져옴
        .sterms()
        .buckets().array().stream()
        // 서버단에서 키워드로 직접 필터링
        .filter(bucket -> bucket.key().stringValue().contains(keyword))
        .map(bucket -> new RegionAutocompleteDTO(bucket.key().stringValue()))
        .collect(Collectors.toList());

    log.info("자동완성 지역 목록: {}",suggestions);

    return ResponseEntity.ok(suggestions);

    //기존 코드 (캠핑장 이름 + 지역) - 우선은 남겨놓음
//    @GetMapping("/autocomplete")
//    public ResponseEntity<List<AutocompleteDTO>> autocomplete(
//        @RequestParam("keyword") String keyword) throws IOException{
//      // "camp_2" 인덱스에서 "doNm" 필드를 대상으로 `match_phrase_prefix` 쿼리를 실행
//      // 이 쿼리는 "입력한 키워드로 시작하는" 구문을 찾아줌
//      SearchResponse<CampNm> response = esClient.search(s -> s
//              .index("camp_2")   // 검색할 인덱스 이름 지정
//              .query(q -> q   // 쿼리 내용 정의 시작
//                  .wildcard(w -> w    // match_phrase_prefix -> wildcard로 변환
//                      .field("doNm")    // .keyword 없이 text 필드 그대로 사용
//                      .value("*" + keyword + "*") // 앞뒤에 와일드카드를 붙여 '포함' 검색 기능으로 추가
//                  )
//              )
//              .size(10),      // 최대 결과 갯수 지정
//          CampNm.class
//      );
//
//    // 검색 결과를 우리가 정의한 DTO 리스트로 변환
//    List<AutocompleteDTO> suggestions = response.hits().hits().stream()
//        .map(hit -> {
//          CampNm campNm = hit.source();
//
//          // 가져온 데이터가 어떤 모양인지 확인 하기 위한 로그 ( 디버깅에 사용 )
//          log.info("변환된 source 객체 : {}", campNm);
//
//          // review가 null일 경우를 대비한 방어코드
//          if (campNm == null){
//            return null;
//          }
//          return new AutocompleteDTO(campNm.getFacltNm(), campNm.getDoNm());
//        })
//        .filter(dto -> dto != null) // null인 경우 제외
//        .collect(Collectors.toList());
//
//    return ResponseEntity.ok(suggestions);


  }

}
