package modackbulz.app.Application.web;

import modackbulz.app.Application.config.auth.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.faq.svc.FaqSVC;
import modackbulz.app.Application.entity.Faq;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage/inquiry")
public class FaqController {

  public final FaqSVC faqSVC;

  // 본인 문의 목록
  @GetMapping
  public String myInquiries(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails == null) return "redirect:/login";

    List<Faq> myList = faqSVC.findByMemberId(userDetails.getMemberId());
    model.addAttribute("inquiries", myList);
    return "member/inquiry";
  }

  // 글쓰기 폼
  @GetMapping("/write")
  public String writeForm() {
    return "member/inquiryWriteForm";
  }

  // 글쓰기 처리
  @PostMapping("/write")
  public String submit(@ModelAttribute Faq faq, @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) return "redirect:/login";

    faq.setMemberId(userDetails.getMemberId());
    faq.setWriter(userDetails.getNickname());
    faqSVC.write(faq);
    return "redirect:/mypage/inquiry";
  }

  // 상세 보기
  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails == null) return "redirect:/login";

    return faqSVC.findById(id)
        .filter(faq ->
            faq.getMemberId().equals(userDetails.getMemberId()))
        .map(faq -> {
          model.addAttribute("faq", faq);
          return "member/inquiryDetail";
        })
        .orElse("redirect:/mypage/inquiry");
  }
}