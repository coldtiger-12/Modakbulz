package modackbulz.app.Application.web;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.autocomplete.dto.CampSearchDto;
import modackbulz.app.Application.domain.autocomplete.svc.SearchSVC;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.review.svc.ReviewSVC;
import modackbulz.app.Application.domain.scrap.dao.CampScrapDAO;
import modackbulz.app.Application.entity.Review;
import org.apache.commons.codec.binary.Hex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/camping")
@RequiredArgsConstructor
public class CampingController {

  private final GoCampingService goCampingService;
  private final CampingDAO campingDAO;
  private final ReviewSVC reviewSVC;
  private final SearchSVC searchSVC;
  private final CampScrapDAO campScrapDAO;
  private final ElasticsearchClient esClient;

  /**
   * 전체 캠핑장 목록 (수정됨: DB 우선 조회)
   */
  @GetMapping
  public String campList(
      @PageableDefault(size = 9, sort = "facltNm") Pageable pageable,
      Model model,
      @AuthenticationPrincipal CustomUserDetails userDetails // <-- 수정: HttpSession 대신 사용
  ) {
    Page<GoCampingDto.Item> campPage = goCampingService.getCampListPageWithImageFallback(pageable).block();

    // --- 추가된 코드 시작 ---
    if (userDetails != null) {
      Long memberId = userDetails.getMemberId();
      for (GoCampingDto.Item item : campPage) {
        boolean scrapped = campScrapDAO.findByMemberIdAndContentId(memberId, item.getContentId()).isPresent();
        item.setScrapped(scrapped);
      }
      model.addAttribute("loginMember", userDetails);
    }
    // --- 추가된 코드 끝 ---

    model.addAttribute("campPage", campPage);
    return "camping/list";
  }

  /**
   * 캠핑장 검색 (수정됨: DB
   * 우선 조회)
   */
  @GetMapping("/search")
  public String searchCamps(
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "region", required = false) String region,
      @RequestParam(name = "theme", required = false) String theme, // theme 파라미터 유지
      @RequestParam(name = "facltNm", required = false) String facltNm,
      @PageableDefault(size = 9) Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      Model model
  ) throws IOException {

    // --- 수정된 로직 시작 ---

    log.info("----- 다중 조건 검색 실행: keyword={}, region={}, facltNm={}, theme={}", keyword, region, facltNm, theme);

    // 1. BoolQuery.Builder를 생성합니다.
    BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

    // 2. 'keyword' 또는 'theme' 조건: 값이 있을 때만 must 조건으로 추가합니다.
    // 두 파라미터 모두 keyword_all 필드를 검색하도록 합니다.
    String mainKeyword = StringUtils.hasText(keyword) ? keyword : theme;
    if (StringUtils.hasText(mainKeyword)) {
      boolQueryBuilder.must(m -> m
          .match(t -> t
              .field("keyword_all")
              .query(mainKeyword)
          )
      );
    }

    // 3. 'region'(지역) 조건: 값이 있을 때만 must 조건으로 추가합니다.
    // addr1 필드에서 지역명을 검색합니다.
    if (StringUtils.hasText(region)) {
      boolQueryBuilder.must(m -> m
          .match(t -> t
              .field("addr1") // 지역 정보가 있는 필드
              .query(region)
          )
      );
    }

    // 4. 'facltNm'(캠핑장 이름) 조건: 값이 있을 때만 must 조건으로 추가합니다.
    if (StringUtils.hasText(facltNm)) {
      boolQueryBuilder.must(m -> m
          .match(t -> t
              .field("facltNm")
              .query(facltNm)
          )
      );
    }

    // 5. 최종 쿼리로 Elasticsearch 검색을 실행합니다.
    SearchResponse<CampSearchDto> response = esClient.search(s -> s
            .index("camping_search")
            .query(q -> q.bool(boolQueryBuilder.build())) // ★ BoolQuery 적용
            .from((int) pageable.getOffset())
            .size(pageable.getPageSize()),
        CampSearchDto.class
    );

    // 검색 결과 DTO 변환 및 페이지 생성
    List<GoCampingDto.Item> items = response.hits().hits().stream()
        .map(hit -> convertCampNmToDtoItem(hit.source()))
        .collect(Collectors.toList());

    Page<GoCampingDto.Item> campPage = new PageImpl<>(items, pageable, response.hits().total().value());

    // 스크랩 여부 설정 (로그인한 경우에만)
    if (userDetails != null) {
      Long memberId = userDetails.getMemberId();
      for (GoCampingDto.Item item : campPage) {
        boolean scrapped = campScrapDAO.findByMemberIdAndContentId(memberId, item.getContentId()).isPresent();
        item.setScrapped(scrapped);
      }
      model.addAttribute("loginMember", userDetails);
    }

    // 모델 속성 추가
    model.addAttribute("campPage", campPage);
    model.addAttribute("keyword", keyword);
    model.addAttribute("region", region);
    model.addAttribute("theme", theme);
    model.addAttribute("facltNm", facltNm);

    return "camping/srcList";
  }


  private GoCampingDto.Item convertCampNmToDtoItem(CampSearchDto campSearchDto){
    if (campSearchDto == null) return null;
    GoCampingDto.Item item = new GoCampingDto.Item();

    item.setContentId(campSearchDto.getContentId().toString());
    item.setFacltNm(campSearchDto.getFacltNm());
    item.setLineIntro(campSearchDto.getLineIntro());
    item.setFirstImageUrl(campSearchDto.getFirstImageUrl());
    item.setAddr1(campSearchDto.getAddr1());
    item.setDoNm(campSearchDto.getDoNm());

    return item;
  }





  /**
   * 캠핑장 상세 정보 (수정됨: DB 우선 조회)
   */
  @GetMapping("/{contentId}")
  public String campDetail(@PathVariable("contentId") Long contentId, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
    // DB 우선 조회 로직이 이미 GoCampingService에 구현되어 있음
    GoCampingDto.Item camp = goCampingService.getCampDetail(contentId).block();

    if (camp != null && userDetails != null) {
      boolean scrapped = campScrapDAO.findByMemberIdAndContentId(userDetails.getMemberId(), camp.getContentId()).isPresent();
      camp.setScrapped(scrapped);
      model.addAttribute("loginMember", userDetails);
    }

    // 캠핑장 정보를 모델에 추가
    model.addAttribute("camp", camp);

    // 해당 캠핑장의 리뷰 목록을 조회하여 모델에 추가
    List<Review> reviews = reviewSVC.findByContentId(contentId);
    model.addAttribute("reviews", reviews);

    // 해당 캠핑장의 리뷰 목록을 조회하여 모델에 추가
    List<Review> review = reviewSVC.findByContentId(contentId);
    model.addAttribute("reviews", reviews);

    // 해당 캠핑장의 키워드 목록을 조회하여 모델에 추가
    List<String> naverKeywords = searchSVC.getNaverKeywordForCamp(contentId);
    model.addAttribute("naverKeywords", naverKeywords);

    // 리뷰 통계 정보 추가 (평점 + 개수)
    Double avgRating = reviewSVC.calculateAverageScore(contentId);
    Map<Integer, Long> distribution = reviewSVC.calculateScoreDistribution(contentId);

    Map<String, Object> reviewStats = new HashMap<>();
    reviewStats.put("avg", avgRating != null ? avgRating : 0.0);
    reviewStats.put("distribution", distribution);
    reviewStats.put("count", reviews.size());
    reviewStats.put("keywords", List.of());

    model.addAttribute("reviewStats", reviewStats);

    // 리뷰 개수 기반 점수 분포 퍼센트 계산
    List<Integer> scorePercent = new ArrayList<>();
    long totalReviews = reviews.size();

    for (int i = 1; i <= 5; i++) {
      long count = distribution.getOrDefault(i, 0L);
      int percent = totalReviews > 0 ? (int) Math.round((double) count / totalReviews * 100) : 0;
      scorePercent.add(percent);
    }
    System.out.println("scorePercent = " + scorePercent);
    System.out.println("size = " + scorePercent.size());
    model.addAttribute("scorePercent", scorePercent);

    // 캠핑장 이미지 목록을 API에서 조회하여 모델에 추가
    if (camp != null) {
      List<String> campImages = goCampingService.getCampImages(contentId).block();

      // 대표 이미지와 campImages 내 중복 제거 (소문자, trim 처리)
      if (campImages != null && camp.getFirstImageUrl() != null && !camp.getFirstImageUrl().isEmpty()) {
        String firstImageUrl = camp.getFirstImageUrl().trim().toLowerCase();
        campImages = campImages.stream()
            .filter(url -> url != null && !url.trim().isEmpty())
            .map(url -> url.trim())
            .filter(url -> !url.equalsIgnoreCase(firstImageUrl))
            .distinct()
            .collect(Collectors.toList());
      } else if (campImages != null) {
        campImages = campImages.stream()
            .filter(url -> url != null && !url.trim().isEmpty())
            .map(url -> url.trim())
            .distinct()
            .collect(Collectors.toList());
      }
      // 실제 이미지 중복 제거
      if (campImages != null && !campImages.isEmpty()) {
        campImages = removeDuplicateImagesByContent(campImages);
      }
      model.addAttribute("campImages", campImages != null ? campImages : Collections.emptyList());
    } else {
      model.addAttribute("campImages", Collections.emptyList());
    }

    return "camping/detail";
  }

  /**
   * 캠핑장 갤러리 전체보기
   */
  @GetMapping("/gallery")
  public String gallery(@RequestParam("campingId") Long campingId, Model model) {
    // 캠핑장 상세 정보 조회
    GoCampingDto.Item camp = goCampingService.getCampDetail(campingId).block();
    model.addAttribute("camp", camp);

    // 캠핑장 이미지 목록 조회
    List<String> campImages = goCampingService.getCampImages(campingId).block();
    // 대표 이미지와 campImages 내 중복 제거 (소문자, trim 처리)
    if (campImages != null && camp != null && camp.getFirstImageUrl() != null && !camp.getFirstImageUrl().isEmpty()) {
      String firstImageUrl = camp.getFirstImageUrl().trim().toLowerCase();
      campImages = campImages.stream()
          .filter(url -> url != null && !url.trim().isEmpty())
          .map(url -> url.trim())
          .filter(url -> !url.equalsIgnoreCase(firstImageUrl))
          .distinct()
          .collect(Collectors.toList());
    } else if (campImages != null) {
      campImages = campImages.stream()
          .filter(url -> url != null && !url.trim().isEmpty())
          .map(url -> url.trim())
          .distinct()
          .collect(Collectors.toList());
    }
    // 실제 이미지 중복 제거
    if (campImages != null && !campImages.isEmpty()) {
      campImages = removeDuplicateImagesByContent(campImages);
    }
    model.addAttribute("campImages", campImages != null ? campImages : Collections.emptyList());
    return "camping/gallery";
  }

  // campImages에서 실제 이미지 중복 제거 (MD5 해시)
  private List<String> removeDuplicateImagesByContent(List<String> imageUrls) {
    Set<Object> hashSet = new HashSet<>();
    List<String> result = new ArrayList<>();
    RestTemplate restTemplate = new RestTemplate();

    for (String url : imageUrls) {
      try {
        byte[] imageBytes = restTemplate.getForObject(url, byte[].class);
        if (imageBytes == null) continue;
        MessageDigest md = MessageDigest.getInstance("MD5");
        String hash = Hex.encodeHexString(md.digest(imageBytes));
        if (hashSet.add(hash)) {
          result.add(url);
        }
      } catch (Exception e) {
        // 실패한 이미지는 무시
      }
    }
    return result;
  }

  @GetMapping("/recommendations") // 클래스의 /camping 과 합쳐져 /camping/recommendations 가 됨
  @ResponseBody // JSON 데이터를 반환하기 위해 필수
  public Mono<List<GoCampingDto.Item>> getCampingRecommendations(
      @RequestParam(name = "region", required = false) String region
  ) {
    int numberOfRecommendations = 8; // 메인에 보여줄 추천 개수

    // 지역 파라미터가 있으면 지역별 추천, 없으면 전체 추천
    if (region != null && !region.trim().isEmpty()) {
      return goCampingService.getRecommendedListByRegion(region.trim(), numberOfRecommendations);
    } else {
      return goCampingService.getRecommendedList(numberOfRecommendations);
    }
  }
}