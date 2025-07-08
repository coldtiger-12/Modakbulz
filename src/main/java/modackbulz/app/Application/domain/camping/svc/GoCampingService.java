package modackbulz.app.Application.domain.camping.svc;

import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
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
  private final CampingDAO campingDAO; // DAO 필드

  /**
   * [수정됨] 생성자에서 CampingDAO를 주입받아 초기화합니다.
   * @param serviceKey application.yml에 설정된 서비스 키
   * @param campingDAO Spring이 자동으로 주입해주는 DAO 객체
   */
  public GoCampingService(@Value("${gocamping.api.service-key}") String serviceKey, CampingDAO campingDAO) {
    this.serviceKey = serviceKey;
    this.campingDAO = campingDAO; // 주입받은 campingDAO를 필드에 할당
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
    return getCampingData("/basedList", "", numOfRows, 1) // path, keyword, numOfRows, pageNo
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
    int pageNo = pageable.getPageNumber() + 1;
    int numOfRows = pageable.getPageSize();

    return getCampingData("/basedList", "", numOfRows, pageNo)
        .map(dto -> {
          List<GoCampingDto.Item> items = Collections.emptyList();
          int totalCount = 0;
          if (dto.getResponse() != null && dto.getResponse().getBody() != null) {
            if (dto.getResponse().getBody().getItems() != null) {
              items = dto.getResponse().getBody().getItems().getItem();
              if (items != null) {
                // API에서 가져온 데이터를 DB에 저장/업데이트합니다.
                items.forEach(campingDAO::saveOrUpdate);
              }
            }
            totalCount = dto.getResponse().getBody().getTotalCount();
          }
          return new PageImpl<>(items, pageable, totalCount);
        })
        .doOnError(error -> log.error("Error while calling GoCamping API", error))
        .onErrorReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  /**
   * 키워드(검색어)로 캠핑장 목록을 검색하는 메서드
   */
  public Mono<PageImpl<GoCampingDto.Item>> searchCampList(String keyword, Pageable pageable) {
    int pageNo = pageable.getPageNumber() + 1;
    int numOfRows = pageable.getPageSize();

    return getCampingData("/searchList", keyword, numOfRows, pageNo)
        .map(dto -> {
          List<GoCampingDto.Item> items = Collections.emptyList();
          int totalCount = 0;
          if (dto.getResponse() != null && dto.getResponse().getBody() != null) {
            if (dto.getResponse().getBody().getItems() != null) {
              items = dto.getResponse().getBody().getItems().getItem();
              if (items != null) {
                // API에서 가져온 데이터를 DB에 저장/업데이트합니다.
                items.forEach(campingDAO::saveOrUpdate);
              }
            }
            totalCount = dto.getResponse().getBody().getTotalCount();
          }
          return new PageImpl<>(items, pageable, totalCount);
        })
        .doOnError(error -> log.error("Error while calling GoCamping search API", error))
        .onErrorReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
  }

  /**
   * contentId로 캠핑장 상세 정보를 조회하는 메서드
   */
  public Mono<GoCampingDto.Item> getCampDetail(Long contentId) {
    return getCampingData("/searchList", String.valueOf(contentId), 1, 1)
        .flatMap(dto -> {
          return Mono.justOrEmpty(Optional.ofNullable(dto)
              .map(GoCampingDto::getResponse)
              .map(GoCampingDto.Response::getBody)
              .map(GoCampingDto.Body::getItems)
              .map(GoCampingDto.Items::getItem)
              .filter(list -> !list.isEmpty())
              .map(list -> list.get(0)));
        })
        .onErrorResume(error -> {
          log.error("GoCamping API 상세 정보 호출 중 에러 발생 (contentId: {}): ", contentId, error);
          return Mono.empty();
        });
  }

  /**
   * GoCamping API 호출 공통 메서드
   */
  private Mono<GoCampingDto> getCampingData(String path, String keyword, int numOfRows, int pageNo) {
    log.info("Requesting GoCamping API: path={}, keyword={}, pageNo={}, numOfRows={}", path, keyword, pageNo, numOfRows);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(path)
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", numOfRows)
            .queryParam("pageNo", pageNo)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "Modakbulz")
            .queryParam("_type", "json")
            .queryParamIfPresent("keyword", Optional.ofNullable(keyword).filter(s -> !s.isBlank()))
            .build())
        .retrieve()
        .bodyToMono(GoCampingDto.class);
  }

}