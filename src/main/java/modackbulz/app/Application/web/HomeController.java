package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.community.dao.CommunityDAO;
import modackbulz.app.Application.entity.Community;
import modackbulz.app.Application.web.form.login.LoginMember;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
  public String home(HttpSession session, Model model) {
    // 추천 캠핑장 8개를 가져오기 위한 Pageable 객체 생성
//    Pageable pageable = PageRequest.of(0, 8);
    // 대표 이미지가 없는 경우를 처리하는 getCampListPageWithImageFallback 메소드 사용
//    List<GoCampingDto.Item> campList = goCampingService.getCampListPageWithImageFallback(pageable).block().getContent();

//    model.addAttribute("recommendCamps", campList);

    // 세션에서 로그인 사용자 정보 추출
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    model.addAttribute("loginMember", loginMember);

    //커뮤니티 최근 글 8개 가져오기
    List<Community> recentPosts = communityDAO.findRecentPosts(8);
    model.addAttribute("recentPosts", recentPosts);

    System.out.println(">> loginMember = " + loginMember);
    return "index";
  }
}