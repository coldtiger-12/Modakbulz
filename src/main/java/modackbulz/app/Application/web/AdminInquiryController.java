package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.faq.svc.FaqSVC;
import modackbulz.app.Application.entity.Faq;
import modackbulz.app.Application.web.form.login.LoginMember;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/inquiry")
public class AdminInquiryController {

  private final FaqSVC faqSVC;

  //관리자 전체 문의 목록
  @GetMapping
  public String list(HttpSession session, Model model) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");

    if(loginMember == null || !"A".equals(loginMember.getGubun())) {
      log.warn("비로그인 사용자의 관리자 페이지 접근 시도");
      return "redirect:/access-denied";
    }
    List<Faq> inquiries = faqSVC.findAll();
    model.addAttribute("inquiries", inquiries);
    return "admin/faqList";
  }
  // 관리자 상세 보기
  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, HttpSession session, Model model) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");

    if (loginMember == null || !"A".equals(loginMember.getGubun())) {
      log.warn("비인가된 문의 상세 접근 시도");
      return "redirect:/access-denied";
    }

    return faqSVC.findById(id)
        .map(faq -> {
          model.addAttribute("faq", faq);
          return "admin/faqDetail"; // templates/admin/faqDetail.html
        })
        .orElse("redirect:/admin/inquiry");
  }

  // 관리자 삭제
  @PostMapping("/delete")
  public String delete(@RequestParam("id") Long id, HttpSession session) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");

    if (loginMember == null || !"A".equals(loginMember.getGubun())) {
      log.warn("비인가된 삭제 시도");
      return "redirect:/access-denied";
    }

    faqSVC.delete(id);
    return "redirect:/admin/inquiry";
  }
}
