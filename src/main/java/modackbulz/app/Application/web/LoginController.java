package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.web.form.login.LoginForm;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

  // 로그인 화면
  @GetMapping("/login")
  public String loginForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails != null) {
      return "redirect:/"; // 이미 로그인된 사용자는 홈으로 리다이렉트
    }
    model.addAttribute("loginForm", new LoginForm());
    return "login/loginForm";
  }

  // 로그아웃 처리는 SecurityConfig에서 담당하므로 컨트롤러에서 별도 구현이 필요 없습니다.
}