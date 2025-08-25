package modackbulz.app.Application.web;

import modackbulz.app.Application.domain.popular.dto.PopularSearchDto;
import modackbulz.app.Application.domain.popular.svc.PopularSearchSvc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PopularSearchController {

  // 👇 로그를 찍기 위한 코드를 추가해주세요.
  private static final Logger log = LoggerFactory.getLogger(PopularSearchController.class);

  private final PopularSearchSvc popularSearchSvc;

  public PopularSearchController(PopularSearchSvc popularSearchSvc) {
    this.popularSearchSvc = popularSearchSvc;
  }

  // 검색 시 호출
  @PostMapping("/search")
  public void searchKeyword(@RequestParam("keyword") String keyword) {
    // 👇 **** 이 로그가 찍히는지 확인하는 것이 가장 중요합니다! ****
    log.info(">>>>>>>>>> [검색어 저장 요청 수신] keyword: {} <<<<<<<<<<", keyword);

    popularSearchSvc.logSearch(keyword);
    // 실제 검색 결과 반환 등은 필요시 추가
  }

  // 인기 검색어 조회
  @GetMapping("/popular-searches")
  public List<PopularSearchDto> getPopularSearches() {
    return popularSearchSvc.getPopularSearches();
  }
}
