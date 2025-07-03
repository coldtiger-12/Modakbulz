package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional; // 이 import 문을 꼭 추가해주세요.

@Controller
@RequiredArgsConstructor
@RequestMapping("/camping")
public class CampingController {

  private final GoCampingService goCampingService;

  @GetMapping
  public String campList(@PageableDefault(size = 9, sort = "facltNm") Pageable pageable, Model model) {
    Page<GoCampingDto.Item> campPage = goCampingService.getCampListPage(pageable).block();
    model.addAttribute("campPage", campPage);
    return "camping/list";
  }

  @GetMapping("/search")
  public String searchCamps(
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "region", required = false) String region,
      @RequestParam(name = "theme", required = false) String theme,
      @PageableDefault(size = 9) Pageable pageable,
      Model model
  ) {
    boolean isEmptySearch = (keyword == null || keyword.isBlank())
        && (region == null || region.isBlank())
        && (theme == null || theme.isBlank());

    PageImpl<GoCampingDto.Item> campPage;

    if (isEmptySearch) {
      campPage = goCampingService.getCampListPage(pageable).block();
    } else {
      StringBuilder sb = new StringBuilder();
      if (keyword != null && !keyword.isBlank()) sb.append(keyword).append(" ");
      if (region != null && !region.isBlank()) sb.append(region).append(" ");
      if (theme != null && !theme.isBlank()) sb.append(theme);

      String finalKeyword = sb.toString().trim();
      campPage = goCampingService.searchCampList(finalKeyword, pageable).block();
    }

    model.addAttribute("campPage", campPage);
    model.addAttribute("keyword", keyword);
    model.addAttribute("region", region);
    model.addAttribute("theme", theme);

    return "camping/srcList";
  }

  /**
   * 캠핑장 상세 정보 페이지
   * @param contentId 캠핑장 콘텐츠 ID
   * @param model 뷰에 전달할 모델
   * @return 상세 정보 페이지 경로
   */
  @GetMapping("/{contentId}")
  public String campDetail(@PathVariable("contentId") String contentId, Model model) {
    // 1. 서비스 로직을 호출하여 캠핑장 상세 정보(Mono)를 받아옵니다.
    // 2. block()의 결과가 null일 수 있으므로 Optional로 안전하게 감싸줍니다.
    Optional<GoCampingDto.Item> campOptional = Optional.ofNullable(goCampingService.getCampDetail(contentId).block());

    // 3. Optional 객체에 값이 실제로 있는지(null이 아닌지) 확인합니다.
    if (campOptional.isPresent()) {
      // 값이 있으면 모델에 'camp'라는 이름으로 담아 뷰로 전달합니다.
      model.addAttribute("camp", campOptional.get());
    } else {
      // 값이 없으면(null이면) 모델에 null을 담습니다.
      // 이렇게 하면 뷰에서 th:if를 사용해 안전하게 처리할 수 있습니다.
      model.addAttribute("camp", null);
    }

    // 4. 'camping/detail.html' 템플릿을 반환합니다.
    return "camping/detail";
  }
}