package modackbulz.app.Application.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class AdminController {

  /**
   * 관리자 대시보드.
   * SecurityConfig에서 이미 "/admin/**" 경로에 대한 접근 제어를 하므로
   * 이 메서드는 관리자 권한이 있는 사용자만 호출할 수 있습니다.
   */
  @GetMapping("/admin")
  public String adminDashboard() {
    log.info("관리자 대시보드에 접근했습니다.");
    return "admin/dashboard";
  }

  @GetMapping("/access-denied")
  public String accessDenied(){
    return "error/accessDenied";
  }
}