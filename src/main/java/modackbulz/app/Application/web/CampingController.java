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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/camping")
public class CampingController {

  private final GoCampingService goCampingService;

  @GetMapping
  public String campList(@PageableDefault(size = 9, sort = "facltNm") Pageable pageable, Model model) {
    // block()을 사용하여 비동기 결과를 동기적으로 기다립니다.
    Page<GoCampingDto.Item> campPage = goCampingService.getCampListPage(pageable).block();
    model.addAttribute("campPage", campPage);
    return "camping/list"; // templates/camping/list.html 뷰를 렌더링
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
      // 조건이 하나도 없으면 전체 목록
      campPage = goCampingService.getCampListPage(pageable).block();
    } else {
      // 조건을 하나라도 넣으면 검색
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
}