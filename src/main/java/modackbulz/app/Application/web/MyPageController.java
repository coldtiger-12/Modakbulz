package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.global.service.EmailService;
import modackbulz.app.Application.web.form.login.LoginMember;
import modackbulz.app.Application.web.form.member.EditForm;
import modackbulz.app.Application.web.form.member.EditForm_Pwd;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

  private final MemberSVC memberSVC;
  private final EmailService emailService; // EmailService 주입

  // 마이페이지 메인 화면 (수정됨)
  @GetMapping
  public String myPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    Optional<Member> memberOptional = memberSVC.findById(loginMember.getId());

    // DB에 회원 정보가 없으면 강제 로그아웃 후 로그인 페이지로 이동
    if (memberOptional.isEmpty()) {
      session.invalidate(); // 세션 무효화
      redirectAttributes.addFlashAttribute("message", "세션이 만료되었거나 회원 정보가 존재하지 않습니다. 다시 로그인해주세요.");
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

  // 회원정보 수정 폼 (수정됨)
  @GetMapping("/edit")
  public String editForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    Optional<Member> memberOptional = memberSVC.findById(loginMember.getId());

    // DB에 회원 정보가 없으면 강제 로그아웃 후 로그인 페이지로 이동
    if (memberOptional.isEmpty()) {
      session.invalidate(); // 세션 무효화
      redirectAttributes.addFlashAttribute("message", "세션이 만료되었거나 회원 정보가 존재하지 않습니다. 다시 로그인해주세요.");
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
      HttpSession session
  ) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    // 이메일 변경 시 인증번호 확인
    String sessionAuthCode = (String) session.getAttribute("authCode");
    if (editForm.getAuthCode() != null && !editForm.getAuthCode().equals(sessionAuthCode)) {
      bindingResult.rejectValue("authCode", "invalid", "인증번호가 일치하지 않습니다.");
    }


    if (bindingResult.hasErrors()) {
      return "member/editForm";
    }

    Member member = new Member();
    member.setMemberId(loginMember.getMemberId());
    member.setNickname(editForm.getNickname());
    member.setTel(editForm.getTel());
    member.setRegion(editForm.getRegion());
    member.setEmail(editForm.getEmail());

    boolean updated = memberSVC.updateMember(member);
    if (!updated) {
      bindingResult.reject("updateFail", "회원정보 수정에 실패했습니다.");
      return "member/editForm";
    }

    // 사용한 인증번호 세션에서 제거
    session.removeAttribute("authCode");
    return "redirect:/mypage?success";
  }

  // 비밀번호 변경 처리
  @PostMapping("/password")
  public String changePassword(
      @Valid @ModelAttribute("editForm_Pwd") EditForm_Pwd editForm_Pwd,
      BindingResult bindingResult,
      HttpSession session,
      RedirectAttributes redirectAttributes) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    if (bindingResult.hasErrors()) {
      return "member/editForm_pwd";
    }

    boolean changed = memberSVC.changePassword(loginMember.getMemberId(), editForm_Pwd.getPwd());

    if(changed) {
      redirectAttributes.addFlashAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
      session.invalidate(); // 비밀번호 변경 후 보안을 위해 로그아웃 처리
      return "redirect:/login";
    } else {
      bindingResult.reject("changePwdFail", "비밀번호 변경에 실패했습니다.");
      return "member/editForm_pwd";
    }
  }

  /**
   * 이메일 인증번호 발송
   */
  @PostMapping("/email/verification-requests")
  public ResponseEntity<String> sendVerificationEmail(@RequestBody Map<String, String> payload, HttpSession session) {
    String email = payload.get("email");
    String authCode = createAuthCode();

    try {
      // 이메일 발송
      String subject = "모닥불즈 이메일 인증번호 입니다.";
      String text = "인증번호: " + authCode;
      emailService.sendEmail(email, subject, text);

      // 인증번호를 세션에 저장
      session.setAttribute("authCode", authCode);
      session.setMaxInactiveInterval(5 * 60); // 세션 유효시간 5분

      return ResponseEntity.ok("인증번호가 발송되었습니다.");
    } catch (Exception e) {
      log.error("이메일 발송 실패", e);
      return ResponseEntity.internalServerError().body("인증번호 발송에 실패했습니다.");
    }
  }

  /**
   * 이메일 인증 번호 확인
   * @param authcode
   * @param session
   * @return
   */
  @PostMapping("verify-email")
  @ResponseBody
  public ResponseEntity<Map<String, Boolean>> verifyEmailCode(
      @RequestParam String authcode, HttpSession session
  ) {
    String sessionAuthCode = (String) session.getAttribute("authCode");
    boolean isVerified = sessionAuthCode != null && sessionAuthCode.equals(authcode);
    session.setAttribute("isEmailVerified", isVerified);
    return ResponseEntity.ok(Map.of("verified", isVerified));
  }

  /**
   * 인증번호 생성
   * @return 6자리 숫자 인증번호
   */
  private String createAuthCode() {
    Random random = new Random();
    int code = 100000 + random.nextInt(900000);
    return String.valueOf(code);
  }


  // 내가 찜한 캠핑장
  @GetMapping("/likes")
  public String likeCampPage(){
    return "member/likeCamp";
  }

  // 내가 쓴 게시글
  @GetMapping("/posts")
  public String myPosts(HttpSession session, Model model) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }
    return "member/myPosts";
  }

  // 문의사항 목록 화면
  @GetMapping("/inquiry")
  public String myInquiries(HttpSession session, Model model) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    return "member/inquiry";
  }

  // 회원 탈퇴 처리
  @PostMapping("/delete")
  public String delete(HttpSession session) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    boolean deleted = memberSVC.deleteMember(loginMember.getMemberId());
    if (!deleted) {
      // 탈퇴 실패 시 처리 로직
      return "redirect:/mypage?error=deleteFail";
    }

    session.invalidate();  // 세션 무효화 (로그아웃 처리)
    return "redirect:/";   // 홈으로 이동
  }
}