package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.dao.MemberDAO;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.web.form.login.LoginForm;
import modackbulz.app.Application.web.form.login.LoginMember;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

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

  // 로그인 처리
  @PostMapping("/login")
  public String login(
      @Valid @ModelAttribute LoginForm loginForm,
      BindingResult bindingResult,
      HttpServletRequest request
  ) {
    log.info("loginForm={}", loginForm);

    // 1. 유효성 검사 오류가 있으면 로그인 폼으로 돌아감
    if (bindingResult.hasErrors()) {
      return "login/loginForm";
    }

    // 2. ID로 회원 정보를 먼저 조회 (login DAO 대신 findById 사용)
    Optional<Member> optionalMember = memberDAO.findById(loginForm.getId());

    // 3. 회원 존재 여부 및 비밀번호 일치 확인
    // passwordEncoder.matches(입력된 비밀번호, DB의 암호화된 비밀번호)
    if (optionalMember.isEmpty() || !passwordEncoder.matches(loginForm.getPwd(), optionalMember.get().getPwd())) {
      bindingResult.reject("loginFail", "아이디 또는 비밀번호가 일치하지 않습니다.");
      return "login/loginForm";
    }

    // 4. 로그인 성공 처리
    Member member = optionalMember.get();
    HttpSession session = request.getSession(true);
    session.setAttribute("loginMember", new LoginMember(
        member.getMemberId(),
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getGubun()
    ));

    return "redirect:/"; // 홈페이지로 리다이렉트
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