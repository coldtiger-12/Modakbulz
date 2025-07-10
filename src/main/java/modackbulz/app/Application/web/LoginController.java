package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.dao.MemberDAO;
import modackbulz.app.Application.web.form.login.LoginForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

  private final MemberDAO memberDAO;
  private final PasswordEncoder passwordEncoder; // PasswordEncoder 주입

  // 로그인 화면
  @GetMapping("/login")
  public String loginForm(HttpServletRequest request,Model model) {
    HttpSession session = request.getSession(false);
    if (session != null && session.getAttribute("loginMember") != null) {
      return "redirect:/"; // 이미 로그인된 경우 홈으로 리다이렉트
    }
    model.addAttribute("loginForm", new LoginForm());
    return "login/loginForm";
  }


  // 로그아웃 처리 (Get 방식 사용 로그인 세션 제거후 url 홈 리다이렉트)
  @GetMapping("/logout")
  public String logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate(); // 세션 제거
    }
    return "redirect:/"; // 홈 페이지로 리다이렉트
  }
}