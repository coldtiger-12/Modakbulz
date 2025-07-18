package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.community.svc.CommunitySVC;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.entity.Community;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.global.service.EmailService;
import modackbulz.app.Application.web.form.member.EditForm;
import modackbulz.app.Application.web.form.member.EditForm_Pwd;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

  private final MemberSVC memberSVC;
  private final EmailService emailService;
  private final CommunitySVC communitySVC;
  private final PasswordEncoder passwordEncoder;

  @ModelAttribute("editForm_Pwd")
  public EditForm_Pwd editFormPwd() {
    return new EditForm_Pwd();
  }

  @GetMapping("/edit")
  public String editForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails == null) return "redirect:/login";

    Optional<Member> memberOptional = memberSVC.findById(userDetails.getUsername());
    if (memberOptional.isEmpty()) return "redirect:/logout";

    Member member = memberOptional.get();
    EditForm editForm = new EditForm();
    BeanUtils.copyProperties(member, editForm);

    model.addAttribute("editForm", editForm);
    return "member/editForm";
  }

  @PostMapping("/edit")
  public String edit(
      @Valid @ModelAttribute("editForm") EditForm editForm,
      BindingResult bindingResult,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      HttpSession session,
      RedirectAttributes redirectAttributes,
      Model model
  ) {
    if (userDetails == null) return "redirect:/login";

    Optional<Member> memberOptional = memberSVC.findById(userDetails.getUsername());
    if (memberOptional.isEmpty()) return "redirect:/logout";
    Member currentMember = memberOptional.get();

    // 닉네임 중복 체크 (자기 자신 제외)
    if (!currentMember.getNickname().equals(editForm.getNickname()) && memberSVC.isExistNickname(editForm.getNickname())) {
      bindingResult.rejectValue("nickname", "duplicate", "이미 사용 중인 닉네임입니다.");
    }

    // 이메일 변경 시 인증번호 확인
    if (!currentMember.getEmail().equals(editForm.getEmail())) {
      VerificationCode sessionCode = (VerificationCode) session.getAttribute("authCode");
      if (sessionCode == null || !sessionCode.isValid(editForm.getAuthCode())) {
        bindingResult.rejectValue("authCode", "invalid", "인증번호가 유효하지 않습니다.");
      }
    }

    if (bindingResult.hasErrors()) {
      model.addAttribute("editForm_Pwd", new EditForm_Pwd());
      return "member/editForm";
    }

    Member memberToUpdate = currentMember;
    memberToUpdate.setNickname(editForm.getNickname());
    memberToUpdate.setTel(editForm.getTel());
    memberToUpdate.setRegion(editForm.getRegion());
    memberToUpdate.setEmail(editForm.getEmail());

    memberSVC.updateMember(memberToUpdate);
    session.removeAttribute("authCode");

    redirectAttributes.addFlashAttribute("success", true);
    return "redirect:/mypage/edit";
  }

  @PostMapping("/password")
  public String changePassword(
      @Valid @ModelAttribute("editForm_Pwd") EditForm_Pwd editForm_Pwd,
      BindingResult bindingResult,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      RedirectAttributes redirectAttributes,
      Model model
  ) {
    if (userDetails == null) return "redirect:/login";

    if (bindingResult.hasErrors()) {
      // 비밀번호 변경 실패 시에도 기존 회원 정보는 다시 모델에 담아줘야 합니다.
      memberSVC.findById(userDetails.getUsername()).ifPresent(member -> {
        EditForm editForm = new EditForm();
        BeanUtils.copyProperties(member, editForm);
        model.addAttribute("editForm", editForm);
      });
      return "member/editForm";
    }

    // 새 비밀번호를 암호화하여 서비스로 전달
    String encryptedPwd = passwordEncoder.encode(editForm_Pwd.getPwd());
    boolean changed = memberSVC.changePassword(userDetails.getMemberId(), encryptedPwd);

    if (changed) {
      redirectAttributes.addFlashAttribute("message", "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.");
      return "redirect:/logout";
    } else {
      redirectAttributes.addFlashAttribute("error", "비밀번호 변경에 실패했습니다.");
      return "redirect:/mypage/edit";
    }
  }

  @PostMapping("/email/verification-requests")
  public ResponseEntity<String> sendVerificationEmail(@RequestBody Map<String, String> payload, HttpSession session) {
    String email = payload.get("email");
    String authCode = createAuthCode();
    try {
      String subject = "모닥불즈 이메일 인증번호 입니다.";
      String text = "인증번호: " + authCode;
      emailService.sendEmail(email, subject, text);
      session.setAttribute("authCode", new VerificationCode(authCode, LocalDateTime.now()));
      return ResponseEntity.ok("인증번호가 발송되었습니다.");
    } catch (Exception e) {
      log.error("이메일 발송 실패", e);
      return ResponseEntity.internalServerError().body("인증번호 발송에 실패했습니다.");
    }
  }

  @GetMapping("/likes")
  public String likeCampPage() {
    return "member/likeCamp";
  }

  @GetMapping("/posts")
  public String myPosts(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails == null) return "redirect:/login";
    List<Community> myPosts = communitySVC.getPostsByMemberId(userDetails.getMemberId());
    model.addAttribute("myPosts", myPosts);
    return "member/myPosts";
  }

  @PostMapping("/delete")
  public String delete(@AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) return "redirect:/login";
    memberSVC.deleteMember(userDetails.getMemberId());
    return "redirect:/logout"; // 탈퇴 성공 시 로그아웃 처리
  }

  private String createAuthCode() {
    Random random = new Random();
    return String.valueOf(100000 + random.nextInt(900000));
  }
}