package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.community.svc.CommunitySVC;
import modackbulz.app.Application.entity.Community;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import modackbulz.app.Application.web.form.login.LoginMember;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

  private final GoCampingService goCampingService; // GoCampingService 주입
  private final CommunitySVC communitySVC; // CommunitySVC 주입

  @GetMapping("/admin")
  public String adminDashboard(HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    //로그인 여부 확인
    if (session == null){
      log.warn("비로그인 사용자의 관리자 페이지 접근 시도");
      return "redirect:/access-denied";
    }

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");

    // 관리자 여부 확인
    if (loginMember == null || !"A".equals(loginMember.getGubun())){
      log.warn("일반 사용자의 관리자 페이지 접근 시도. ID={}",
          loginMember != null ? loginMember.getId() : "비회원");
      return "redirect:/access-denied";
    }

    //관리자 인증 완료
    log.info("관리자 접근 성공: ID={}, 닉네임={}", loginMember.getId(), loginMember.getNickname());
    return "admin/posts";
  }

  @GetMapping("/access-denied")
  public String accessDenied(){
    return "error/accessDenied";  // templates/error/accessDenied.html
  }

  /**
   * [추가] 캠핑장 데이터 전체 동기화를 수동으로 실행하는 엔드포인트
   */
  @PostMapping("/admin/sync-camps")
  public ResponseEntity<String> syncCamps() {
    goCampingService.syncAllCampingDataFromApi().subscribe(); // 비동기 실행
    return ResponseEntity.ok("캠핑장 데이터 동기화 작업이 시작되었습니다.");
  }

  // 관리자 게시글 목록
  @GetMapping("/admin/posts")
  public String adminPosts(Model model) {
    try {
      List<Community> posts = communitySVC.getAllPosts();
      log.info("관리자 게시글 목록 조회: {}개", posts != null ? posts.size() : 0);
      model.addAttribute("posts", posts != null ? posts : List.of());
    } catch (Exception e) {
      log.error("게시글 목록 조회 중 오류 발생", e);
      model.addAttribute("posts", List.of());
      model.addAttribute("msg", "게시글 목록을 불러오는 중 오류가 발생했습니다.");
    }
    return "admin/posts";
  }

  // 관리자 게시글 상세
  @GetMapping("/admin/posts/{id}")
  public String adminPostDetail(@PathVariable("id") Long id, Model model) {
    Community post = communitySVC.getPostById(id).orElse(null);
    if (post == null) {
      model.addAttribute("msg", "해당 게시글이 존재하지 않습니다.");
      return "redirect:/admin/posts";
    }
    model.addAttribute("post", post);
    return "admin/postDetail";
  }

  // 관리자 게시글 수정 폼
  @GetMapping("/admin/posts/{id}/edit")
  public String adminPostEditForm(@PathVariable("id") Long id, Model model) {
    Community post = communitySVC.getPostById(id).orElse(null);
    if (post == null) {
      model.addAttribute("msg", "해당 게시글이 존재하지 않습니다.");
      return "redirect:/admin/posts";
    }
    model.addAttribute("communityForm", post);
    return "admin/postEditForm";
  }

  // 관리자 게시글 수정 처리
  @PostMapping("/admin/posts/{id}/edit")
  public String adminPostEdit(@PathVariable("id") Long id, Community communityForm) {
    communityForm.setCoId(id);
    communitySVC.updatePost(communityForm);
    return "redirect:/admin/posts/" + id;
  }

  // 관리자 게시글 삭제
  @PostMapping("/admin/posts/{id}/delete")
  public String adminPostDelete(@PathVariable("id") Long id) {
    communitySVC.deletePost(id);
    return "redirect:/admin/posts";
  }

  // 게시글 일괄 삭제
  @PostMapping("/admin/posts/bulk-delete")
  public String bulkDeletePosts(@RequestParam("postIds") List<Long> postIds) {
    if (postIds != null) {
      for (Long id : postIds) {
        communitySVC.deletePost(id);
      }
    }
    return "redirect:/admin/posts";
  }

  @GetMapping("/admin-test")
  public String testAdminDirect() {
    return "admin/posts";
  }


}
