package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.faq.svc.FaqSVC;
import modackbulz.app.Application.entity.Faq;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/inquiry")
public class AdminInquiryController {

  private final FaqSVC faqSVC;

  //관리자 전체 문의 목록
  @GetMapping
  public String list(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    // Spring Security가 이미 관리자(A) 역할만 허용하므로, userDetails는 null이 될 수 없습니다.
    // 하지만 안전을 위해 null 체크를 유지할 수 있습니다.
    List<Faq> inquiries = faqSVC.findAll();
    model.addAttribute("inquiries", inquiries);
    return "admin/faqList";
  }
  // 관리자 상세 보기
  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    // SecurityConfig에서 이미 관리자만 접근 가능하도록 설정되어 있습니다.
    return faqSVC.findById(id)
        .map(faq -> {
          model.addAttribute("faq", faq);
          return "admin/faqDetail"; // templates/admin/faqDetail.html
        })
        .orElse("redirect:/admin/inquiry");
  }

  // 관리자 삭제
  @PostMapping("/delete")
  public String delete(@RequestParam("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
    // SecurityConfig에서 이미 관리자만 접근 가능하도록 설정되어 있습니다.
    faqSVC.delete(id);
    return "redirect:/admin/inquiry";
  }
}