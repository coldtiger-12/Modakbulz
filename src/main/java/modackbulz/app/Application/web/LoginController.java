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

    // 이미 로그인한 사용자라면 홈으로 리다이렉트
    if (userDetails != null) {

      return "redirect:/";
    }
    model.addAttribute("loginForm", new LoginForm());
    return "login/loginForm";
  }
}