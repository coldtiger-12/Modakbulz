package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.global.service.EmailService;
import modackbulz.app.Application.web.form.member.EditForm;
import modackbulz.app.Application.web.form.member.EditForm_Pwd;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

  private final MemberSVC memberSVC;
  private final EmailService emailService;
  private final PasswordEncoder passwordEncoder;

  // VerificationCode 내부 클래스 삭제

  // 회원정보 수정 폼
  @GetMapping({"", "/edit"})
  public String editForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
    if (userDetails == null) return "redirect:/login";
    Optional<Member> memberOptional = memberSVC.findById(userDetails.getUsername());
    if (memberOptional.isEmpty()) {
      redirectAttributes.addFlashAttribute("message", "회원 정보가 존재하지 않습니다. 다시 로그인해주세요.");
      return "redirect:/login";
    }
    Member member = memberOptional.get();
    EditForm editForm = new EditForm();
    editForm.setId(member.getId());
    editForm.setNickname(member.getNickname());
    editForm.setTel(member.getTel());
    editForm.setRegion(member.getRegion());
    editForm.setEmail(member.getEmail());
    model.addAttribute("editForm", editForm);
    model.addAttribute("editForm_Pwd", new EditForm_Pwd());
    return "member/editForm";
  }

  // 회원정보 수정 처리
  @PostMapping("/edit")
  public String edit(
      @Valid @ModelAttribute("editForm") EditForm editForm,
      BindingResult bindingResult,
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    if (userDetails == null) return "redirect:/login";
    if (bindingResult.hasErrors()) return "member/editForm";
    Optional<Member> memberOptional = memberSVC.findById(userDetails.getUsername());
    if(memberOptional.isEmpty()) return "redirect:/login";
    Member memberToUpdate = memberOptional.get();
    memberToUpdate.setNickname(editForm.getNickname());
    memberToUpdate.setTel(editForm.getTel());
    memberToUpdate.setRegion(editForm.getRegion());
    memberToUpdate.setEmail(editForm.getEmail());
    boolean updated = memberSVC.updateMember(memberToUpdate);
    if (!updated) {
      bindingResult.reject("updateFail", "회원정보 수정에 실패했습니다.");
      return "member/editForm";
    }
    return "redirect:/mypage?success";
  }

  // 비밀번호 변경 처리
  @PostMapping("/password")
  public String changePassword(
      @Valid @ModelAttribute("editForm_Pwd") EditForm_Pwd editForm_Pwd,
      BindingResult bindingResult,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      RedirectAttributes redirectAttributes,
      HttpSession session
  ) {
    Boolean isVerified = (Boolean) session.getAttribute("emailVerified");
    if (isVerified == null || !isVerified) {
      bindingResult.reject("unauthorizedChange", "이메일 인증이 완료되지 않았습니다. 인증을 먼저 진행해주세요.");
      return "member/editForm";
    }

    if (userDetails == null) return "redirect:/login";
    if (bindingResult.hasErrors()) return "member/editForm";

    Optional<Member> memberOptional = memberSVC.findById(userDetails.getUsername());
    if(memberOptional.isEmpty()) return "redirect:/login";

    Member member = memberOptional.get();
    boolean changed = memberSVC.changePassword(member.getMemberId(), passwordEncoder.encode(editForm_Pwd.getPwd()));

    session.removeAttribute("emailVerified");

    if (changed) {
      redirectAttributes.addFlashAttribute("message", "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.");
      return "redirect:/login";
    } else {
      bindingResult.reject("changePwdFail", "비밀번호 변경에 실패했습니다.");
      return "member/editForm";
    }
  }

  // 이메일 인증번호 발송 처리
  @PostMapping("/email/verification-requests")
  public ResponseEntity<String> sendVerificationEmail(@RequestBody Map<String, String> payload, HttpSession session) {
    String email = payload.get("email");
    SecureRandom random = new SecureRandom();
    int authCode = 100000 + random.nextInt(900000);

    try {
      String subject = "[모닥불즈] 이메일 인증 번호 안내";
      String text = "인증 번호는 " + authCode + " 입니다. 이 번호는 5분간 유효합니다.";
      emailService.sendEmail(email, subject, text);
      // 새로 만든 VerificationCode 클래스 사용
      session.setAttribute("authCode", new VerificationCode(String.valueOf(authCode), LocalDateTime.now()));
      log.info("인증번호 발송 완료. 이메일: {}, 인증번호: {}", email, authCode);
      return ResponseEntity.ok("인증번호가 발송되었습니다.");
    } catch (Exception e) {
      log.error("이메일 발송 실패. 이메일: {}", email, e);
      return ResponseEntity.internalServerError().body("인증번호 발송에 실패했습니다.");
    }
  }

  // 이메일 인증번호 확인 처리
  @PostMapping("/verify-email")
  public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("authcode") String authCode, HttpSession session) {
    // 새로 만든 VerificationCode 클래스 사용
    VerificationCode sessionCode = (VerificationCode) session.getAttribute("authCode");

    if (sessionCode == null) {
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 발급되지 않았습니다."));
    }

    if (sessionCode.isValid(authCode)) {
      session.removeAttribute("authCode");
      session.setAttribute("emailVerified", true);
      return ResponseEntity.ok(Map.of("verified", true, "message", "인증에 성공했습니다."));
    } else {
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 올바르지 않거나 유효시간이 초과되었습니다."));
    }
  }

  // 내가 찜한 캠핑장
  @GetMapping("/likes")
  public String likeCampPage() {
    return "member/likeCamp";
  }

  // 내가 쓴 게시글
  @GetMapping("/posts")
  public String myPosts() {
    return "member/myPosts";
  }

  // 문의사항 목록 화면
  @GetMapping("/inquiry")
  public String myInquiries() {
    return "member/inquiry";
  }

  // 회원 탈퇴 처리
  @PostMapping("/delete")
  public String delete(@AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
    if (userDetails == null) return "redirect:/login";
    Optional<Member> memberOptional = memberSVC.findById(userDetails.getUsername());
    if(memberOptional.isEmpty()) return "redirect:/login";
    boolean deleted = memberSVC.deleteMember(memberOptional.get().getMemberId());
    if (!deleted) {
      redirectAttributes.addFlashAttribute("error", "deleteFail");
      return "redirect:/mypage";
    }
    return "redirect:/logout";
  }
}