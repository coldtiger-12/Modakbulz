package modackbulz.app.Application.domain.camping.svc;

import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GoCampingService {

  private final WebClient webClient;
  private final String serviceKey;

  public GoCampingService(@Value("${gocamping.api.service-key}") String serviceKey) {
    this.serviceKey = serviceKey;
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://apis.data.go.kr/B551011/GoCamping");
    factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
    this.webClient = WebClient.builder()
        .uriBuilderFactory(factory)
        .baseUrl("http://apis.data.go.kr/B551011/GoCamping")
        .build();
  }

  /**
   * 메인페이지 추천 캠핑장 목록 (기존 메서드)
   */
  public Mono<List<GoCampingDto.Item>> getBasedList(int numOfRows) {
    return getCampingData(1, numOfRows)
        .map(dto -> Optional.ofNullable(dto)
            .map(GoCampingDto::getResponse)
            .map(GoCampingDto.Response::getBody)
            .map(GoCampingDto.Body::getItems)
            .map(GoCampingDto.Items::getItem)
            .orElse(Collections.emptyList()))
        .doOnError(error -> log.error("GoCamping API 'getBasedList' 호출 중 오류 발생", error))
        .onErrorResume(error -> Mono.just(Collections.emptyList()));
  }


  /**
   * 캠핑장 목록 페이지 (페이지네이션 적용)
   */
  public Mono<PageImpl<GoCampingDto.Item>> getCampListPage(Pageable pageable) {
    int pageNo = pageable.getPageNumber() + 1; // API는 페이지 번호가 1부터 시작
    int numOfRows = pageable.getPageSize();

    return getCampingData(pageNo, numOfRows)
        .map(dto -> {
          List<GoCampingDto.Item> items = Collections.emptyList();
          int totalCount = 0;
          if (dto.getResponse() != null && dto.getResponse().getBody() != null) {
            if (dto.getResponse().getBody().getItems() != null) {
              items = dto.getResponse().getBody().getItems().getItem();
            }
            totalCount = dto.getResponse().getBody().getTotalCount();
          }
          return new PageImpl<>(items, pageable, totalCount);
        })
        .doOnError(error -> log.error("Error while calling GoCamping API", error))
        .onErrorReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  /**
   * GoCamping API 호출 공통 메서드
   */
  private Mono<GoCampingDto> getCampingData(int pageNo, int numOfRows) {
    log.info("Requesting GoCamping API: pageNo={}, numOfRows={}", pageNo, numOfRows);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/basedList")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", numOfRows)
            .queryParam("pageNo", pageNo)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "Modakbulz")
            .queryParam("_type", "json")
            .build())
        .retrieve()
        .bodyToMono(GoCampingDto.class);
  }

  /**
   * 키워드(검색어)로 캠핑장 목록을 검색하는 메서드
   *
   * @param keyword 검색어 (캠핑장명, 지역명 등)
   * @param pageable 페이지 정보 (페이지 번호, 사이즈 등)
   * @return 검색 결과를 포함한 페이지 객체 (Mono로 비동기 반환)
   */
  public Mono<PageImpl<GoCampingDto.Item>> searchCampList(String keyword, Pageable pageable) {
    int pageNo = pageable.getPageNumber() + 1; // API의 pageNo는 1부터 시작
    int numOfRows = pageable.getPageSize();    // 한 페이지에 가져올 항목 수

    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/searchList")                         // 고캠핑 API의 검색용 엔드포인트
            .queryParam("serviceKey", serviceKey)        // 인증키
            .queryParam("keyword", keyword)              // 검색어
            .queryParam("numOfRows", numOfRows)          // 페이지당 항목 수
            .queryParam("pageNo", pageNo)                // 페이지 번호
            .queryParam("MobileOS", "ETC")               // 고정값
            .queryParam("MobileApp", "Modakbulz")        // 앱 이름
            .queryParam("_type", "json")                 // 응답 형식
            .build())
        .retrieve()
        .bodyToMono(GoCampingDto.class)                  // JSON → DTO로 변환
        .map(dto -> {
          List<GoCampingDto.Item> items = Collections.emptyList();
          int totalCount = 0;

          // 응답 객체가 정상적으로 왔는지 확인하고, 데이터 꺼내기
          if (dto.getResponse() != null && dto.getResponse().getBody() != null) {
            if (dto.getResponse().getBody().getItems() != null) {
              items = dto.getResponse().getBody().getItems().getItem();
            }
            totalCount = dto.getResponse().getBody().getTotalCount();
          }

          // PageImpl 객체로 반환
          return new PageImpl<>(items, pageable, totalCount);
        })
        .doOnError(error -> log.error("Error while calling GoCamping search API", error))
        .onErrorReturn(new PageImpl<>(Collections.emptyList(), pageable, 0)); // 에러 시 빈 결과 반환
  }
}