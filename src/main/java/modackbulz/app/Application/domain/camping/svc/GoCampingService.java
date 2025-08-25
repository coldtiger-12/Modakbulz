package modackbulz.app.Application.domain.camping.svc;

import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
import modackbulz.app.Application.domain.camping.dto.CampingImageDto;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoCampingService {

  private final WebClient webClient;
  private final String serviceKey;
  private final CampingDAO campingDAO;
  private final String baseUrl = "http://apis.data.go.kr/B551011/GoCamping";

  /**
   * [최종 수정] WebClient를 기본 생성하고, 모든 API 호출에 URI 템플릿 방식을 사용합니다.
   */
  public GoCampingService(@Value("${gocamping.api.service-key}") String serviceKey, CampingDAO campingDAO) {
    this.serviceKey = serviceKey; // 원본(디코딩된) 키를 주입
    this.campingDAO = campingDAO;

    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();

    this.webClient = WebClient.builder()
        .exchangeStrategies(exchangeStrategies)
        .build();
  }

  /**
   * [최종 수정] URI 템플릿을 사용하여 가장 표준적이고 안정적인 방식으로 API를 호출합니다.
   */
  private Mono<GoCampingDto> getCampingData(String path, String keyword, int numOfRows, int pageNo) {
    log.info("Requesting GoCamping API: path={}, keyword={}, pageNo={}, numOfRows={}", path, keyword, pageNo, numOfRows);

    String url = this.baseUrl + path +
        "?serviceKey={serviceKey}&numOfRows={numOfRows}&pageNo={pageNo}" +
        "&MobileOS={MobileOS}&MobileApp={MobileApp}&_type=json";

    Map<String, Object> uriVariables = new HashMap<>();
    uriVariables.put("serviceKey", this.serviceKey);
    uriVariables.put("numOfRows", numOfRows);
    uriVariables.put("pageNo", pageNo);
    uriVariables.put("MobileOS", "ETC");
    uriVariables.put("MobileApp", "Modakbulz");

    if (keyword != null && !keyword.isBlank()) {
      url += "&keyword={keyword}";
      uriVariables.put("keyword", keyword);
    }

    return webClient.get()
        .uri(url, uriVariables)
        .retrieve()
        .bodyToMono(GoCampingDto.class);
  }

  /**
   * [최종 수정] 이미지 API도 URI 템플릿 방식으로 수정합니다.
   */
  private Mono<CampingImageDto> getCampingImages(String path, Long contentId) {
    log.info("Requesting GoCamping Image API: path={}, contentId={}", path, contentId);

    String url = this.baseUrl + path +
        "?serviceKey={serviceKey}&contentId={contentId}" +
        "&MobileOS={MobileOS}&MobileApp={MobileApp}&_type=json";

    Map<String, Object> uriVariables = new HashMap<>();
    uriVariables.put("serviceKey", this.serviceKey);
    uriVariables.put("contentId", contentId);
    uriVariables.put("MobileOS", "ETC");
    uriVariables.put("MobileApp", "Modakbulz");

    return webClient.get()
        .uri(url, uriVariables)
        .retrieve()
        .bodyToMono(CampingImageDto.class);
  }

  public Mono<List<GoCampingDto.Item>> getBasedList(int numOfRows) {
    Page<GoCampingDto.Item> dbPage = campingDAO.findAll(Pageable.ofSize(numOfRows));
    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      log.info("DB에서 캠핑장 목록을 조회합니다. (개수: {})", dbPage.getContent().size());
      return Mono.just(dbPage.getContent());
    } else {
      log.info("DB에 데이터가 없어 API에서 캠핑장 목록을 조회합니다.");
      return getCampingData("/basedList", "", numOfRows, 1)
          .map(dto -> Optional.ofNullable(dto)
              .map(GoCampingDto::getResponse)
              .map(GoCampingDto.Response::getBody)
              .map(GoCampingDto.Body::getItems)
              .map(GoCampingDto.Items::getItem)
              .orElse(Collections.emptyList()))
          .doOnNext(items -> {
            if (items != null && !items.isEmpty()) {
              items.forEach(campingDAO::saveOrUpdate);
              log.info("API에서 가져온 {}개의 캠핑장 데이터를 DB에 저장했습니다.", items.size());
            }
          })
          .doOnError(error -> log.error("GoCamping API 'getBasedList' 호출 중 오류 발생", error))
          .onErrorResume(error -> Mono.just(Collections.emptyList()));
    }
  }

  public Mono<PageImpl<GoCampingDto.Item>> getCampListPage(Pageable pageable) {
    Page<GoCampingDto.Item> dbPage = campingDAO.findAll(pageable);
    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      log.info("DB에서 캠핑장 목록을 조회합니다. (페이지: {}, 개수: {})",
          pageable.getPageNumber(), dbPage.getContent().size());
      return Mono.just(new PageImpl<>(dbPage.getContent(), pageable, dbPage.getTotalElements()));
    } else {
      log.info("DB에 데이터가 없어 API에서 캠핑장 목록을 조회합니다.");
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
                  items.forEach(campingDAO::saveOrUpdate);
                  log.info("API에서 가져온 {}개의 캠핑장 데이터를 DB에 저장했습니다.", items.size());
                }
              }
              totalCount = dto.getResponse().getBody().getTotalCount();
            }
            return new PageImpl<>(items, pageable, totalCount);
          })
          .doOnError(error -> log.error("Error while calling GoCamping API", error))
          .onErrorReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
    }
  }

  public Mono<PageImpl<GoCampingDto.Item>> getCampListPageWithImageFallback(Pageable pageable) {
    return getCampListPage(pageable)
        .flatMap(page -> {
          List<GoCampingDto.Item> items = page.getContent();
          return Flux.fromIterable(items)
              .flatMap(item -> {
                if (item.getFirstImageUrl() != null && !item.getFirstImageUrl().isEmpty()) {
                  return Mono.just(item);
                } else {
                  return getCampImages(item.getContentId())
                      .map(images -> {
                        if (images != null && !images.isEmpty()) {
                          item.setFirstImageUrl(images.get(0));
                        }
                        return item;
                      });
                }
              })
              .collectList()
              .map(newItems -> new PageImpl<>(newItems, pageable, page.getTotalElements()));
        });
  }

  public Mono<PageImpl<GoCampingDto.Item>> searchCampList(String keyword, Pageable pageable) {
    Page<GoCampingDto.Item> dbPage = campingDAO.search(keyword, pageable);
    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      log.info("DB에서 키워드 '{}'로 캠핑장을 검색합니다. (개수: {})", keyword, dbPage.getContent().size());
      return Mono.just(new PageImpl<>(dbPage.getContent(), pageable, dbPage.getTotalElements()));
    } else {
      log.info("DB에 검색 결과가 없어 API에서 키워드 '{}'로 캠핑장을 검색합니다.", keyword);
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
                  items.forEach(campingDAO::saveOrUpdate);
                  log.info("API에서 가져온 {}개의 캠핑장 데이터를 DB에 저장했습니다.", items.size());
                }
              }
              totalCount = dto.getResponse().getBody().getTotalCount();
            }
            return new PageImpl<>(items, pageable, totalCount);
          })
          .doOnError(error -> log.error("Error while calling GoCamping search API", error))
          .onErrorReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));
    }
  }

  public Mono<GoCampingDto.Item> getCampDetail(Long contentId) {
    Optional<GoCampingDto.Item> dbItem = campingDAO.findByContentId(contentId);
    if (dbItem.isPresent()) {
      log.info("DB에서 캠핑장 상세 정보를 조회합니다. (contentId: {})", contentId);
      return Mono.just(dbItem.get());
    } else {
      log.info("DB에 데이터가 없어 API에서 캠핑장 상세 정보를 조회합니다. (contentId: {})", contentId);
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
          .doOnNext(item -> {
            if (item != null) {
              campingDAO.saveOrUpdate(item);
              log.info("API에서 가져온 캠핑장 상세 정보를 DB에 저장했습니다. (contentId: {})", contentId);
            }
          })
          .onErrorResume(error -> {
            log.error("GoCamping API 상세 정보 호출 중 에러 발생 (contentId: {}): ", contentId, error);
            return Mono.empty();
          });
    }
  }

  public Mono<List<String>> getCampImages(Long contentId) {
    return getCampingImages("/imageList", contentId)
        .map(dto -> Optional.ofNullable(dto)
            .map(CampingImageDto::getResponse)
            .map(CampingImageDto.Response::getBody)
            .map(CampingImageDto.Body::getItems)
            .map(CampingImageDto.Items::getItem)
            .map(items -> items.stream()
                .map(CampingImageDto.Item::getImageUrl)
                .filter(url -> url != null && !url.isEmpty())
                .distinct()
                .limit(10)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList()))
        .doOnError(error -> log.error("GoCamping API 이미지 목록 호출 중 에러 발생 (contentId: {}): ", contentId, error))
        .onErrorResume(error -> Mono.just(Collections.emptyList()));
  }

  public Mono<Integer> syncAllCampingDataFromApi() {
    log.info("========== 전체 캠핑장 정보 동기화 시작 ==========");
    return getCampingData("/basedList", "", 1, 1)
        .flatMap(initialDto -> {
          int totalCount = initialDto.getResponse().getBody().getTotalCount();
          if (totalCount == 0) {
            log.warn("API에서 가져올 데이터가 없습니다. 동기화를 종료합니다.");
            return Mono.just(0);
          }
          int numOfRows = 100;
          int totalPages = (int) Math.ceil((double) totalCount / numOfRows);
          log.info("총 데이터: {}건, 페이지당 {}건, 총 페이지: {}", totalCount, numOfRows, totalPages);
          AtomicInteger savedCount = new AtomicInteger(0);
          return Flux.range(1, totalPages)
              .delayElements(java.time.Duration.ofMillis(100))
              .doOnNext(pageNo -> log.info("페이지 {}/{} 처리 시작...", pageNo, totalPages))
              .flatMap(pageNo -> getCampingData("/basedList", "", numOfRows, pageNo)
                  .map(dto -> {
                    List<GoCampingDto.Item> items = Optional.ofNullable(dto.getResponse())
                        .map(GoCampingDto.Response::getBody)
                        .map(GoCampingDto.Body::getItems)
                        .map(GoCampingDto.Items::getItem)
                        .orElse(Collections.emptyList());
                    if (items.isEmpty()) {
                      log.warn("페이지 {}에서 유효한 아이템을 찾을 수 없습니다.", pageNo);
                    } else {
                      log.info("페이지 {}에서 {}개의 아이템을 가져왔습니다. DB 저장 시작...", pageNo, items.size());
                    }
                    return items;
                  })
                  .doOnError(e -> log.error("페이지 {} API 호출 실패", pageNo, e))
                  .onErrorResume(e -> Mono.just(Collections.emptyList()))
              )
              .flatMap(Flux::fromIterable)
              .doOnNext(item -> {
                campingDAO.saveOrUpdate(item);
                savedCount.incrementAndGet();
              })
              .count()
              .map(count -> savedCount.get());
        })
        .doOnSuccess(count -> log.info("========== 전체 캠핑장 정보 동기화 완료! 총 {}건 저장 ==========", count))
        .doOnError(error -> log.error("========== 동기화 작업 중 심각한 오류 발생 ==========", error));
  }

  @Scheduled(cron = "0 0 4 * * *")
  public void scheduleSync() {
    log.info("캠핑장 정보 자동 동기화 스케줄을 시작합니다.");
    syncAllCampingDataFromApi().subscribe(
        count -> log.info("스케줄된 동기화 작업 완료. 총 {}건 처리.", count),
        error -> log.error("스케줄된 동기화 작업 중 오류 발생", error)
    );
  }

  public Mono<List<GoCampingDto.Item>> getRecommendedList(int numOfRows) {
    Page<GoCampingDto.Item> dbPage = campingDAO.findAllOrderByScrapCountDesc(Pageable.ofSize(numOfRows));
    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      log.info("DB에서 스크랩 순 추천 캠핑장 목록을 조회합니다. (개수: {})", dbPage.getContent().size());
      return Mono.just(dbPage.getContent());
    } else {
      log.warn("스크랩된 캠핑장이 없어 최신순으로 목록을 반환합니다.");
      return getBasedList(numOfRows);
    }
  }

  /**
   * 지역으로 필터링된 추천 캠핑장 목록을 반환합니다.
   * @param region    필터링할 지역 이름
   * @param numOfRows 가져올 개수
   * @return 캠핑장 목록 Mono
   */
  public Mono<List<GoCampingDto.Item>> getRecommendedListByRegion(String region, int numOfRows) {
    Pageable pageable = Pageable.ofSize(numOfRows);
    Page<GoCampingDto.Item> dbPage = campingDAO.findAllByRegionOrderByScrapCountDesc(region, pageable);

    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      log.info("DB에서 '{}' 지역의 스크랩 순 추천 캠핑장 목록을 조회합니다. (개수: {})", region, dbPage.getContent().size());
      return Mono.just(dbPage.getContent());
    } else {
      log.warn("'{}' 지역에 추천할 캠핑장이 없습니다. 빈 목록을 반환합니다.", region);
      return Mono.just(Collections.emptyList()); // 결과가 없을 경우 비어있는 리스트 반환
    }
  }
}