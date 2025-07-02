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

  // 500 에러를 해결하기 위해 수정된 부분입니다.
  @GetMapping("/{contentId}")
  public String campDetail(@PathVariable("contentId") String contentId, Model model) {
    // block()의 결과가 null일 수 있으므로 Optional로 안전하게 감싸줍니다.
    Optional<GoCampingDto.Item> campOptional = Optional.ofNullable(goCampingService.getCampDetail(contentId).block());

    // Optional 객체에 값이 실제로 들어있는지(null이 아닌지) 확인합니다.
    if (campOptional.isPresent()) {
      // 값이 있으면 모델에 담아서 detail.html로 전달합니다.
      model.addAttribute("camp", campOptional.get());
    } else {
      // 값이 없으면(null이면) 모델에 null을 담습니다.
      // 이렇게 하면 detail.html의 th:if 조건문이 이를 처리하여 500 에러 대신 "정보 없음" 페이지를 보여줍니다.
      model.addAttribute("camp", null);
    }
    return "camping/detail";
  }
}