package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.web.form.login.LoginMember;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

  private final GoCampingService goCampingService; // GoCampingService 주입

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
    return "admin/dashboard";   // templates/admin/dashboard.html
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

}
