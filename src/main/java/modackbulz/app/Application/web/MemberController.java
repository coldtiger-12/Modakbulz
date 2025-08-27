package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.global.service.EmailService;
import modackbulz.app.Application.web.form.member.JoinForm;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

  private final MemberSVC memberSVC;
  private final EmailService emailService;

  @GetMapping("/join-type")
  public String joinType() {
    return "member/joinType";
  }

  @GetMapping("/join-user")
  public String userJoinForm(Model model) {
    model.addAttribute("joinForm", new JoinForm());
    model.addAttribute("gubun", "U");
    return "member/joinForm";
  }

  @GetMapping("/join-admin")
  public String adminJoinForm(Model model) {
    model.addAttribute("joinForm", new JoinForm());
    model.addAttribute("gubun", "A");
    return "member/joinForm";
  }

  @PostMapping("/join")
  public String join(
      @Valid @ModelAttribute("joinForm") JoinForm joinForm,
      BindingResult bindingResult,
      HttpSession session,
      Model model,
      @RequestParam("gubun") String gubun
  ) {
    if (!joinForm.getPwd().equals(joinForm.getPwdChk())){
      bindingResult.addError(new FieldError("joinForm","pwdchk","비밀번호와 비밀번호 확인이 일치하지 않습니다."));
    }

    if (bindingResult.hasErrors()) {
      model.addAttribute("gubun", gubun);
      return "member/joinForm";
    }

    if (memberSVC.isExist(joinForm.getId())) {
      bindingResult.rejectValue("id", "duplicate", "이미 등록된 아이디입니다.");
    }
    if (memberSVC.isExistNickname(joinForm.getNickname())) {
      bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
    }

    Boolean isVerified = (Boolean) session.getAttribute("joinEmailVerified");
    if (isVerified == null || !isVerified) {
      bindingResult.reject("joinFail", "이메일 인증이 필요합니다.");
    }

    if (bindingResult.hasErrors()) {
      model.addAttribute("gubun", gubun);
      return "member/joinForm";
    }

    Member member = new Member();
    BeanUtils.copyProperties(joinForm, member);
    member.setGubun(gubun);

    try {
      memberSVC.insertMember(member);
    } catch (Exception e) {
      log.error("회원가입 실패", e);
      bindingResult.reject("joinFail", "회원가입 처리 중 오류가 발생했습니다.");
      model.addAttribute("gubun", gubun);
      return "member/joinForm";
    }

    session.removeAttribute("joinEmailVerified");
    session.removeAttribute("authCode");
    return "redirect:/login";
  }

  @PostMapping("/check-nickname")
  @ResponseBody
  public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestBody Map<String, String> payload) {
    String nickname = payload.get("nickname");
    boolean isAvailable = !memberSVC.isExistNickname(nickname);
    return ResponseEntity.ok(Map.of("isAvailable", isAvailable));
  }

  @PostMapping("/email/verification-requests")
  public ResponseEntity<String> sendVerificationEmail(@RequestBody Map<String, String> payload, HttpSession session) {
    String email = payload.get("email");
    String authCode = createAuthCode();
    try {
      String subject = "모닥불즈 회원가입 이메일 인증번호 입니다.";
      String text = "인증번호: " + authCode;
      emailService.sendEmail(email, subject, text);
      session.setAttribute("authCode", new VerificationCode(authCode, LocalDateTime.now()));
      return ResponseEntity.ok("인증번호가 발송되었습니다.");
    } catch (Exception e) {
      log.error("이메일 발송 실패", e);
      return ResponseEntity.internalServerError().body("인증번호 발송에 실패했습니다.");
    }
  }

  @PostMapping("/verify-email")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> verifyEmailCode(
      @RequestParam("authcode") String authcode, HttpSession session) {
    VerificationCode sessionCode = (VerificationCode) session.getAttribute("authCode");

    if (sessionCode == null) {
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호를 먼저 발송해주세요."));
    }

    if (sessionCode.isValid(authcode)) {
      session.setAttribute("joinEmailVerified", true);
      return ResponseEntity.ok(Map.of("verified", true, "message", "인증되었습니다."));
    } else {
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 다르거나 유효시간이 지났습니다."));
    }
  }

  @GetMapping("/processLogoutAfterAction")
  public String processLogoutAfterAction(
      @ModelAttribute("message") String message,
      Model model){
    model.addAttribute("message", message);
    return "member/processLogout";
  }

  /**
   * 랜덤 인증번호 생성기
   * @return 랜덤 번호
   */
  private String createAuthCode() {
    Random random = new Random();
    return String.valueOf(100000 + random.nextInt(900000));
  }
}