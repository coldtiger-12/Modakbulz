package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.community.dao.CommunityDAO;
import modackbulz.app.Application.entity.Community;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;

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

    // 각 캠핑장별로 대표이미지가 없으면 갤러리의 첫 번째 이미지를 대표이미지로 세팅하고, campImages(최소 1장)를 추가
    if (campList != null) {
      for (GoCampingDto.Item camp : campList) {
        List<String> campImages = goCampingService.getCampImages(camp.getContentId()).block();
        if (camp.getFirstImageUrl() == null || camp.getFirstImageUrl().isEmpty()) {
          if (campImages != null && !campImages.isEmpty()) {
            camp.setFirstImageUrl(campImages.get(0));
          }
        }
        // campImages 필드가 없다면 동적으로 추가(런타임), 있다면 세팅
        try {
          java.lang.reflect.Field field = camp.getClass().getDeclaredField("campImages");
          field.setAccessible(true);
          field.set(camp, campImages != null ? campImages : new ArrayList<>());
        } catch (NoSuchFieldException e) {
          // campImages 필드가 없으면 무시
        } catch (IllegalAccessException e) {
          // 무시
        }
      }
    }

//    // 해시태그 생성
//    if (campList != null && !campList.isEmpty()) {
//      Set<String> hashtags = new HashSet<>();
//
//      for (GoCampingDto.Item camp : campList) {
//        //테마환경(themaEnvrnCl)필드에서 태그 추출
//        if (camp.getThemaEnvrnCl() != null && !camp.getThemaEnvrnCl().isBlank()) {
//          hashtags.addAll(Arrays.asList(camp.getThemaEnvrnCl().split(",")));
//        }
//      }
//      model.addAttribute("hashtags",hashtags.stream().limit(7).collect(Collectors.toList()));
//    }

    // 변환된 리스트를 모델에 추가합니다.
    model.addAttribute("recommendCamps", campList);

    //커뮤니티 최근 글 8개 가져오기
    List<Community> recentPosts = communityDAO.findRecentPosts(8);
    model.addAttribute("recentPosts", recentPosts);

    return "index";
  }
}