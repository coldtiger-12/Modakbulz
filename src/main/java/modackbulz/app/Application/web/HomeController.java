package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.community.dao.CommunityDAO;
import modackbulz.app.Application.entity.Community;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

  private final GoCampingService goCampingService;
  private final CommunityDAO communityDAO;

  @GetMapping("/")
  public String home(Model model) {
    // goCampingService.getBasedList()가 반환하는 Mono<List<...>>를
    // .block()을 호출하여 List<...>로 변환합니다.
    // 메인에 표시할 캠핑장 개수를 8개로 지정합니다.
    List<GoCampingDto.Item> campList = goCampingService.getBasedList(8).block();

    // 변환된 리스트를 모델에 추가합니다.
    model.addAttribute("recommendCamps", campList);

    //커뮤니티 최근 글 8개 가져오기
    List<Community> recentPosts = communityDAO.findRecentPosts(8);
    model.addAttribute("recentPosts", recentPosts);

    return "index";
  }
}