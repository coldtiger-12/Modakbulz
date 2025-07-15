package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.global.service.EmailService;
import modackbulz.app.Application.web.form.member.JoinForm;
// 새로 만든 VerificationCode 클래스를 import
import modackbulz.app.Application.web.VerificationCode;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
  private final MemberSVC memberSVC;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  // 회원가입 폼
  @GetMapping("/join")
  public String joinForm(Model model) {
    model.addAttribute("joinForm", new JoinForm());
    return "member/joinForm";
  }

  // 회원가입 처리
  @PostMapping("/join")
  public String join(
      @Valid @ModelAttribute JoinForm joinForm,
      BindingResult bindingResult,
      HttpSession session
  ) {
    log.info("회원가입 요청: {}", joinForm);

    if (bindingResult.hasErrors()) {
      return "member/joinForm";
    }

    if (memberSVC.isExistNickname(joinForm.getNickname())) {
      bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
      return "member/joinForm";
    }

    Boolean isVerified = (Boolean) session.getAttribute("joinEmailVerified");
    if (isVerified == null || !isVerified) {
      bindingResult.rejectValue("email", "email.unverified", "이메일 인증이 필요합니다.");
      return "member/joinForm";
    }

    if (memberSVC.isExist(joinForm.getId())) {
      bindingResult.rejectValue("id", "duplicate", "이미 등록된 아이디입니다.");
      return "member/joinForm";
    }

    Member member = new Member();
    BeanUtils.copyProperties(joinForm, member);

    member.setPwd(passwordEncoder.encode(joinForm.getPwd()));
    member.setIsDel("N");
    member.setDelDate(null);
    member.setGubun("U");

    try {
      memberSVC.insertMember(member);
      session.removeAttribute("joinEmailVerified");
    } catch (Exception e) {
      log.error("회원가입 실패", e);
      bindingResult.reject("joinFail", "회원가입 처리 중 오류가 발생했습니다.");
      return "member/joinForm";
    }

    return "redirect:/login";
  }

  // 닉네임 중복 확인 API
  @PostMapping("/check-nickname")
  public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestBody Map<String, String> payload) {
    boolean isAvailable = !memberSVC.isExistNickname(payload.get("nickname"));
    return ResponseEntity.ok(Map.of("isAvailable", isAvailable));
  }

  /**
   * 회원가입용 이메일 인증번호 발송 API
   */
  @PostMapping("/email/verification-requests")
  public ResponseEntity<String> sendVerificationEmail(@RequestBody Map<String, String> payload, HttpSession session) {
    String email = payload.get("email");
    SecureRandom random = new SecureRandom();
    int authCode = 100000 + random.nextInt(900000);

    try {
      String subject = "[모닥불즈] 회원가입 이메일 인증 번호 안내";
      String text = "인증 번호는 " + authCode + " 입니다. 이 번호는 5분간 유효합니다.";
      emailService.sendEmail(email, subject, text);

      // 새로 만든 VerificationCode 클래스 사용
      session.setAttribute("authCode", new VerificationCode(String.valueOf(authCode), LocalDateTime.now()));
      log.info("회원가입 인증번호 발송 완료. 이메일: {}, 인증번호: {}", email, authCode);
      return ResponseEntity.ok("인증번호가 발송되었습니다.");
    } catch (Exception e) {
      log.error("이메일 발송 실패. 이메일: {}", email, e);
      return ResponseEntity.internalServerError().body("인증번호 발송에 실패했습니다.");
    }
  }

  /**
   * 회원가입용 이메일 인증번호 확인 API
   */
  @PostMapping("/verify-email")
  public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("authcode") String authCode, HttpSession session) {
    // 새로 만든 VerificationCode 클래스 사용
    VerificationCode sessionCode = (VerificationCode) session.getAttribute("authCode");

    if (sessionCode == null) {
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 발급되지 않았습니다."));
    }

    if (sessionCode.isValid(authCode)) {
      session.removeAttribute("authCode");
      session.setAttribute("joinEmailVerified", true);
      return ResponseEntity.ok(Map.of("verified", true, "message", "인증에 성공했습니다."));
    } else {
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 올바르지 않거나 유효시간이 초과되었습니다."));
    }
  }
}