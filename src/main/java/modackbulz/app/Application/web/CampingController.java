package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;

import java.util.*;
import java.util.stream.Collectors;

import modackbulz.app.Application.domain.review.svc.ReviewSVC;
import modackbulz.app.Application.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import org.springframework.web.client.RestTemplate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/camping")
public class CampingController {

  private final GoCampingService goCampingService;
  private final CampingDAO campingDAO;
  private final ReviewSVC reviewSVC;

  /**
   * 전체 캠핑장 목록 (수정됨: DB 우선 조회)
   * 1. DB에서 데이터를 먼저 조회합니다.
   * 2. DB에 데이터가 없으면 API를 호출하여 DB에 저장합니다.
   */
  @GetMapping
  public String campList(@PageableDefault(size = 9, sort = "facltNm") Pageable pageable, Model model) {
    // DB 우선 조회 로직이 이미 GoCampingService에 구현되어 있음
    Page<GoCampingDto.Item> campPage = goCampingService.getCampListPageWithImageFallback(pageable).block();
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
      Model model
  ) {
    // region, theme 파라미터는 현재 DAO에서 사용하지 않으므로 keyword만 사용
    String finalKeyword = (keyword != null ? keyword : "")
        + (region != null ? " " + region : "")
        + (theme != null ? " " + theme : "");
    finalKeyword = finalKeyword.trim();

    Page<GoCampingDto.Item> campPage;

    if (finalKeyword.isBlank()) {
      // 키워드가 없으면 전체 목록 조회 (DB 우선 조회 로직 사용)
      campPage = goCampingService.getCampListPage(pageable).block();
    } else {
      // 키워드가 있으면 검색 (DB 우선 조회 로직 사용)
      campPage = goCampingService.searchCampList(finalKeyword, pageable).block();
    }

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
  public String campDetail(@PathVariable("contentId") Long contentId, Model model) {
    // DB 우선 조회 로직이 이미 GoCampingService에 구현되어 있음
    GoCampingDto.Item camp = goCampingService.getCampDetail(contentId).block();

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

  /**
   * [추가] 메인 페이지 추천 캠핑장 목록을 비동기로 제공하는 API
   */
  @GetMapping("/recommendations")
  @ResponseBody // HTML 뷰가 아닌 JSON 데이터를 직접 반환하도록 설정
  public ResponseEntity<List<GoCampingDto.Item>> getRecommendations() {
    Pageable pageable = PageRequest.of(0, 8); // 8개 항목 조회
    List<GoCampingDto.Item> campList = goCampingService.getCampListPageWithImageFallback(pageable).block().getContent();
    return ResponseEntity.ok(campList); // 조회된 데이터를 JSON 형태로 반환
  }
}