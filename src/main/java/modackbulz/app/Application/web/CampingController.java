package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
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

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/camping")
public class CampingController {

  private final GoCampingService goCampingService;
  private final CampingDAO campingDAO;

  /**
   * 전체 캠핑장 목록 (수정됨)
   * 1. API를 호출하여 최신 데이터를 DB에 저장/업데이트합니다.
   * 2. DB에서 페이징 처리된 데이터를 가져와 화면에 보여줍니다.
   */
  @GetMapping
  public String campList(@PageableDefault(size = 9, sort = "facltNm") Pageable pageable, Model model) {
    // 1. 서비스를 호출하고, 서비스가 반환한 Page 객체를 직접 사용합니다.
    // getCampListPage 메서드는 API 호출, DB 저장, Page 객체 생성을 모두 책임집니다.
    Page<GoCampingDto.Item> campPage = goCampingService.getCampListPage(pageable).block();

    // 2. 서비스로부터 받은 Page 객체를 모델에 추가합니다. (DB 재조회 로직 삭제)
    model.addAttribute("campPage", campPage);
    return "camping/list";
  }

  /**
   * 캠핑장 검색 (수정됨)
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
      // 키워드가 없으면 전체 목록 조회
      goCampingService.getCampListPage(pageable).block();
      campPage = campingDAO.findAll(pageable);
    } else {
      // 키워드가 있으면 검색 API 호출로 DB 업데이트 후, DB에서 검색
      goCampingService.searchCampList(finalKeyword, pageable).block();
      campPage = campingDAO.search(finalKeyword, pageable);
    }

    model.addAttribute("campPage", campPage);
    model.addAttribute("keyword", keyword);
    model.addAttribute("region", region);
    model.addAttribute("theme", theme);

    return "camping/srcList";
  }

  /**
   * 캠핑장 상세 정보 (DB 우선 조회)
   */
  @GetMapping("/{contentId}")
  public String campDetail(@PathVariable("contentId") Long contentId, Model model) {
    // DB에서 먼저 조회를 시도합니다.
    Optional<GoCampingDto.Item> campOptional = campingDAO.findByContentId(contentId);

    if (campOptional.isPresent()) {
      // DB에 정보가 있으면 바로 모델에 담습니다.
      model.addAttribute("camp", campOptional.get());
    } else {
      // DB에 정보가 없으면 API를 호출하여 조회하고 DB에 저장합니다.
      GoCampingDto.Item itemFromApi = goCampingService.getCampDetail(contentId).block();
      if(itemFromApi != null) {
        campingDAO.saveOrUpdate(itemFromApi);
      }
      model.addAttribute("camp", itemFromApi);
    }
    return "camping/detail";
  }
}