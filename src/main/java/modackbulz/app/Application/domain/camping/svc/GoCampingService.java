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
        .map(dto -> dto.getResponse().getBody().getItems().getItem());
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
}