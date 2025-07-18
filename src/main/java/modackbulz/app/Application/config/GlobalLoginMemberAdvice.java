package modackbulz.app.Application.config;

import jakarta.servlet.http.HttpSession;
import modackbulz.app.Application.web.form.login.LoginMember;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalLoginMemberAdvice {

  @ModelAttribute("loginMember")
  public LoginMember addLoginMember(HttpSession session) {
    return (LoginMember) session.getAttribute("loginMember");
  }
}