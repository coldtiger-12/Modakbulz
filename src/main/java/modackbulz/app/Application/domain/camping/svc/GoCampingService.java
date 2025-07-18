package modackbulz.app.Application.domain.camping.svc;

import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.dto.CampingImageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoCampingService {

  private final WebClient webClient;
  private final String serviceKey;
  private final CampingDAO campingDAO; // DAO 필드
  private boolean isInitialized = false; // 초기화 여부를 추적하는 플래그

  /**
   * [수정됨] 생성자에서 CampingDAO를 주입받아 초기화하고, WebClient의 버퍼 크기를 늘립니다.
   * @param serviceKey application.yml에 설정된 서비스 키
   * @param campingDAO Spring이 자동으로 주입해주는 DAO 객체
   */
  public GoCampingService(@Value("${gocamping.api.service-key}") String serviceKey, CampingDAO campingDAO) {
    this.serviceKey = serviceKey;
    this.campingDAO = campingDAO;

    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://apis.data.go.kr/B551011/GoCamping");
    factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

    // WebClient가 더 큰 응답을 처리할 수 있도록 버퍼 크기 설정
    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB로 증설
        .build();

    this.webClient = WebClient.builder()
        .uriBuilderFactory(factory)
        .exchangeStrategies(exchangeStrategies) // 수정된 전략 적용
        .baseUrl("http://apis.data.go.kr/B551011/GoCamping")
        .build();
  }

  /**
   * 메인페이지 추천 캠핑장 목록 (수정됨: DB 우선 조회)
   */
  public Mono<List<GoCampingDto.Item>> getBasedList(int numOfRows) {
    // DB에서 데이터를 먼저 조회
    Page<GoCampingDto.Item> dbPage = campingDAO.findAll(Pageable.ofSize(numOfRows));

    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      // DB에 데이터가 있으면 DB에서 반환
      log.info("DB에서 캠핑장 목록을 조회합니다. (개수: {})", dbPage.getContent().size());
      return Mono.just(dbPage.getContent());
    } else {
      // DB에 데이터가 없으면 API 호출 (첫 실행 시에만)
      log.info("DB에 데이터가 없어 API에서 캠핑장 목록을 조회합니다.");
      return getCampingData("/basedList", "", numOfRows, 1)
          .map(dto -> Optional.ofNullable(dto)
              .map(GoCampingDto::getResponse)
              .map(GoCampingDto.Response::getBody)
              .map(GoCampingDto.Body::getItems)
              .map(GoCampingDto.Items::getItem)
              .orElse(Collections.emptyList()))
          .doOnNext(items -> {
            // API에서 가져온 데이터를 DB에 저장
            if (items != null && !items.isEmpty()) {
              items.forEach(campingDAO::saveOrUpdate);
              log.info("API에서 가져온 {}개의 캠핑장 데이터를 DB에 저장했습니다.", items.size());
            }
          })
          .doOnError(error -> log.error("GoCamping API 'getBasedList' 호출 중 오류 발생", error))
          .onErrorResume(error -> Mono.just(Collections.emptyList()));
    }
  }

  /**
   * 캠핑장 목록 페이지 (수정됨: DB 우선 조회)
   */
  public Mono<PageImpl<GoCampingDto.Item>> getCampListPage(Pageable pageable) {
    // DB에서 데이터를 먼저 조회
    Page<GoCampingDto.Item> dbPage = campingDAO.findAll(pageable);

    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      // DB에 데이터가 있으면 DB에서 반환
      log.info("DB에서 캠핑장 목록을 조회합니다. (페이지: {}, 개수: {})",
          pageable.getPageNumber(), dbPage.getContent().size());
      return Mono.just(new PageImpl<>(dbPage.getContent(), pageable, dbPage.getTotalElements()));
    } else {
      // DB에 데이터가 없으면 API 호출 (첫 실행 시에만)
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
                  // API에서 가져온 데이터를 DB에 저장/업데이트합니다.
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

  /**
   * 대표 이미지 보정: 대표 이미지가 없으면 imageList에서 첫 번째 이미지를 대표 이미지로 사용
   */
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

  /**
   * 키워드(검색어)로 캠핑장 목록을 검색하는 메서드 (수정됨: DB 우선 조회)
   */
  public Mono<PageImpl<GoCampingDto.Item>> searchCampList(String keyword, Pageable pageable) {
    // DB에서 먼저 검색
    Page<GoCampingDto.Item> dbPage = campingDAO.search(keyword, pageable);

    if (dbPage != null && !dbPage.getContent().isEmpty()) {
      // DB에 검색 결과가 있으면 DB에서 반환
      log.info("DB에서 키워드 '{}'로 캠핑장을 검색합니다. (개수: {})", keyword, dbPage.getContent().size());
      return Mono.just(new PageImpl<>(dbPage.getContent(), pageable, dbPage.getTotalElements()));
    } else {
      // DB에 검색 결과가 없으면 API 호출
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
                  // API에서 가져온 데이터를 DB에 저장/업데이트합니다.
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

  /**
   * contentId로 캠핑장 상세 정보를 조회하는 메서드 (수정됨: DB 우선 조회)
   */
  public Mono<GoCampingDto.Item> getCampDetail(Long contentId) {
    // DB에서 먼저 조회
    Optional<GoCampingDto.Item> dbItem = campingDAO.findByContentId(contentId);

    if (dbItem.isPresent()) {
      // DB에 데이터가 있으면 DB에서 반환
      log.info("DB에서 캠핑장 상세 정보를 조회합니다. (contentId: {})", contentId);
      return Mono.just(dbItem.get());
    } else {
      // DB에 데이터가 없으면 API 호출
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
            // API에서 가져온 데이터를 DB에 저장
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

  /**
   * contentId로 캠핑장 이미지 목록을 조회하는 메서드
   */
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
                .distinct() // 중복 URL 제거
                .limit(10) // 최대 10개까지만 표시 (성능 고려)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList()))
        .doOnError(error -> log.error("GoCamping API 이미지 목록 호출 중 에러 발생 (contentId: {}): ", contentId, error))
        .onErrorResume(error -> Mono.just(Collections.emptyList()));
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

  /**
   * GoCamping 이미지 API 호출 메서드
   */
  private Mono<CampingImageDto> getCampingImages(String path, Long contentId) {
    log.info("Requesting GoCamping Image API: path={}, contentId={}", path, contentId);
    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(path)
            .queryParam("serviceKey", serviceKey)
            .queryParam("contentId", contentId)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "Modakbulz")
            .queryParam("_type", "json")
            .build())
        .retrieve()
        .bodyToMono(CampingImageDto.class);
  }

  /**
   * [추가] GoCamping API의 모든 캠핑장 정보를 DB에 동기화하는 메서드
   * @return 저장된 총 캠핑장 개수
   */
  public Mono<Integer> syncAllCampingDataFromApi() {
    log.info("========== 전체 캠핑장 정보 동기화 시작 ==========");
    // 1. 첫 페이지를 호출하여 전체 데이터 개수(totalCount)를 가져옵니다.
    return getCampingData("/basedList", "", 1, 1)
        .flatMap(initialDto -> {
          int totalCount = initialDto.getResponse().getBody().getTotalCount();
          if (totalCount == 0) {
            log.warn("API에서 가져올 데이터가 없습니다. 동기화를 종료합니다.");
            return Mono.just(0);
          }

          // 2. 필요한 총 페이지 수를 계산합니다. (한 페이지에 100개씩 가져온다고 가정)
          int numOfRows = 100;
          int totalPages = (int) Math.ceil((double) totalCount / numOfRows);
          log.info("총 데이터: {}건, 페이지당 {}건, 총 페이지: {}", totalCount, numOfRows, totalPages);

          AtomicInteger savedCount = new AtomicInteger(0);

          // 3. 모든 페이지에 대해 API를 비동기적으로 호출합니다.
          return Flux.range(1, totalPages) // 1페이지부터 totalPages까지 반복
              .delayElements(java.time.Duration.ofMillis(100)) // API 과부하 방지를 위한 딜레이 (50ms -> 100ms)
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
                  .onErrorResume(e -> Mono.just(Collections.emptyList())) // 오류 발생 시 해당 페이지만 건너뛰기
              )
              .flatMap(Flux::fromIterable) // List<Item> -> Flux<Item>
              .doOnNext(item -> {
                campingDAO.saveOrUpdate(item);
                savedCount.incrementAndGet();
              })
              .count() // 처리된 총 아이템 개수 세기
              .map(count -> savedCount.get());
        })
        .doOnSuccess(count -> log.info("========== 전체 캠핑장 정보 동기화 완료! 총 {}건 저장 ==========", count))
        .doOnError(error -> log.error("========== 동기화 작업 중 심각한 오류 발생 ==========", error));
  }

  /**
   * [추가] 매일 새벽 4시에 전체 캠핑장 정보를 동기화하는 스케줄러
   */
  @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시에 실행
  public void scheduleSync() {
    log.info("캠핑장 정보 자동 동기화 스케줄을 시작합니다.");
    syncAllCampingDataFromApi().subscribe(
        count -> log.info("스케줄된 동기화 작업 완료. 총 {}건 처리.", count),
        error -> log.error("스케줄된 동기화 작업 중 오류 발생", error)
    );
  }
}