package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.review.svc.ReviewSVC;
import modackbulz.app.Application.domain.scrap.dao.CampScrapDAO;
import modackbulz.app.Application.entity.Review;
import org.apache.commons.codec.binary.Hex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/camping")
public class CampingController {

  private final GoCampingService goCampingService;
  private final CampingDAO campingDAO;
  private final ReviewSVC reviewSVC;
  private final CampScrapDAO campScrapDAO;

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
   * 캠핑장 검색 (수정됨: DB 우선 조회)
   */
  @GetMapping("/search")
  public String searchCamps(
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "region", required = false) String region,
      @RequestParam(name = "theme", required = false) String theme,
      @PageableDefault(size = 9) Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      Model model
  ) {
    // 로그인 여부 확인
    boolean isLoggedIn = (userDetails != null);
    model.addAttribute("loginMember", isLoggedIn ? userDetails : null);

    // 검색어 조합 처리
    String finalKeyword = (keyword != null ? keyword : "")
        + (region != null ? " " + region : "")
        + (theme != null ? " " + theme : "");
    finalKeyword = finalKeyword.trim();

    // 캠핑장 검색 or 전체 목록
    Page<GoCampingDto.Item> campPage;
    if (finalKeyword.isBlank()) {
      campPage = goCampingService.getCampListPage(pageable).block();
    } else {
      campPage = goCampingService.searchCampList(finalKeyword, pageable).block();
    }

    // 스크랩 여부 설정 (로그인한 경우에만)
    if (isLoggedIn) {
      Long memberId = userDetails.getMemberId();
      for (GoCampingDto.Item item : campPage) {
        boolean scrapped = campScrapDAO.findByMemberIdAndContentId(memberId, item.getContentId()).isPresent();
        item.setScrapped(scrapped);
      }
    }

    // 모델 속성 추가
    model.addAttribute("campPage", campPage);
    model.addAttribute("keyword", keyword);
    model.addAttribute("region", region);
    model.addAttribute("theme", theme);

    return "camping/srcList";
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
  public Mono<List<GoCampingDto.Item>> getCampingRecommendations() {
    int numberOfRecommendations = 8; // 메인에 보여줄 추천 개수
    // 이전에 만든 '스크랩순 정렬' 서비스 메소드를 호출하도록 변경
    return goCampingService.getRecommendedList(numberOfRecommendations);
  }
}