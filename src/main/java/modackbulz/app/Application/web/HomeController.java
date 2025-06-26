package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto; // Dto 임포트 추가
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List; // List 임포트 추가

@Controller
@RequiredArgsConstructor
public class HomeController {

  private final GoCampingService goCampingService;

  @GetMapping("/")
  public String home(Model model) {
    // goCampingService.getBasedList(2)가 반환하는 Mono<List<...>>를
    // .block()을 호출하여 List<...>로 변환합니다.
    List<GoCampingDto.Item> campList = goCampingService.getBasedList(2).block();

    // 변환된 리스트를 모델에 추가합니다.
    model.addAttribute("recommendCamps", campList);

    return "index";
  }
}