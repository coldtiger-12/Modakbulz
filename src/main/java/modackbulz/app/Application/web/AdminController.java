package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.community.svc.CommunitySVC;
import modackbulz.app.Application.entity.Community;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin") // 클래스 레벨에서 /admin 경로를 공통으로 사용
public class AdminController {

  private final GoCampingService goCampingService;
  private final CommunitySVC communitySVC;

  // SecurityConfig에 의해 이 컨트롤러의 모든 메소드는 ROLE_A 권한이 필요합니다.
  @GetMapping
  public String adminDashboard() {
    return "redirect:/admin/posts";
  }

//  @GetMapping("/access-denied")
//  public String accessDenied(){
//    return "error/accessDenied";
//  }

  @PostMapping("/sync-camps")
  public ResponseEntity<String> syncCamps() {
    goCampingService.syncAllCampingDataFromApi().subscribe();
    return ResponseEntity.ok("캠핑장 데이터 동기화 작업이 시작되었습니다.");
  }

  @GetMapping("/posts")
  public String adminPosts(Model model) {
    try {
      List<Community> posts = communitySVC.getAllPosts();
      model.addAttribute("posts", posts);
    } catch (Exception e) {
      log.error("관리자 게시글 목록 조회 중 오류 발생", e);
      model.addAttribute("posts", List.of());
      model.addAttribute("msg", "게시글 목록을 불러오는 중 오류가 발생했습니다.");
    }
    return "admin/posts";
  }

  @GetMapping("/posts/{id}")
  public String adminPostDetail(@PathVariable("id") Long id, Model model) {
    communitySVC.getPostById(id).ifPresentOrElse(
        post -> model.addAttribute("post", post),
        () -> model.addAttribute("msg", "해당 게시글이 존재하지 않습니다.")
    );
    return "admin/postDetail";
  }

  @GetMapping("/posts/{id}/edit")
  public String adminPostEditForm(@PathVariable("id") Long id, Model model) {
    communitySVC.getPostById(id).ifPresentOrElse(
        post -> model.addAttribute("communityForm", post),
        () -> model.addAttribute("msg", "해당 게시글이 존재하지 않습니다.")
    );
    return "admin/postEditForm";
  }

  @PostMapping("/posts/{id}/edit")
  public String adminPostEdit(@PathVariable("id") Long id, Community communityForm) {
    communityForm.setCoId(id);
    communitySVC.updatePost(communityForm);
    return "redirect:/admin/posts/" + id;
  }

  @PostMapping("/posts/{id}/delete")
  public String adminPostDelete(@PathVariable("id") Long id) {
    communitySVC.deletePost(id);
    return "redirect:/admin/posts";
  }

  @PostMapping("/posts/bulk-delete")
  public String bulkDeletePosts(@RequestParam("postIds") List<Long> postIds) {
    if (postIds != null) {
      postIds.forEach(communitySVC::deletePost);
    }
    return "redirect:/admin/posts";
  }
}