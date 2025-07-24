package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.community.svc.CommunitySVC;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.domain.scrap.svc.CampScrapService;
import modackbulz.app.Application.entity.CampScrap;
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
  private final CampScrapService campScrapService;

  // 👇 [추가된 코드] /mypage 요청을 /mypage/edit으로 리다이렉트합니다.
  @GetMapping
  public String mypageRoot() {
    return "redirect:/mypage/edit";
  }

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
      Model model, HttpSession session
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
      session.removeAttribute("isEmailVerifiedForPasswordChange");
      // [최종 수정] POST 방식으로 로그아웃을 실행할 전용 페이지로 이동
      return "member/processLogout";
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

  @PostMapping("/verify-email")
  public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam("authcode") String authcode, HttpSession session) {
    // 새로 만든 VerificationCode 클래스 사용
    VerificationCode sessionCode = (VerificationCode) session.getAttribute("authCode");

    if (sessionCode == null) {
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 발급 되지 않았습니다."));
    }
    if (sessionCode.isValid(authcode)){
      //비밀번호 변경을 위한 인증이 완료되었음을 세션에 기록
      session.setAttribute("isEmailVerifiedForPasswordChange", true);
      return ResponseEntity.ok(Map.of("verified", true, "message", "인증에 성공했습니다."));
    } else {
      log.warn("이메일 인증 실패. 세션 코드: {}, 입력 코드: {}", sessionCode.getCode(), authcode);
      return ResponseEntity.ok(Map.of("verified", false, "message", "인증번호가 올바르지 않거나 유효시간이 초과되었습니다."));
    }
  }

  @GetMapping("/likes")
  public String likeCampPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails == null) {
      return "redirect:/login";
    }
    List<CampScrap> myScraps = campScrapService.getMyScraps(userDetails.getMemberId());
    model.addAttribute("myScraps", myScraps);
    return "member/likeCamp";
  }

  @GetMapping("/posts")
  public String myPosts(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    if (userDetails == null) return "redirect:/login";
    List<Community> myPosts = communitySVC.getPostsByMemberId(userDetails.getMemberId());
    model.addAttribute("myPosts", myPosts);
    return "member/myPosts";
  }

//  @PostMapping("/delete")
//  public String delete(@AuthenticationPrincipal CustomUserDetails userDetails) {
//    if (userDetails == null) return "redirect:/login";
//    memberSVC.deleteMember(userDetails.getMemberId());
//    return "redirect:/logout"; // 탈퇴 성공 시 로그아웃 처리
//  }

  /**
   * 회원 탈퇴 요청 처리
   * @param userDetails
   * @param redirectAttributes
   * @return 탈퇴 요청 처리 성공 여부
   */
  @PostMapping("/withdraw")
  public String withdraw(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      RedirectAttributes redirectAttributes){
    // 로그인 상태 확인
    if (userDetails == null){
      redirectAttributes.addAttribute("message", "로그인이 필요한 기능입니다.");
      return "redirect:/login";
    }

    // 현재 로그인한 사용자 id 가져오기
    Long memberId = userDetails.getMemberId();

    boolean result = memberSVC.requestDeletion(memberId);
    if (result){
      redirectAttributes.addFlashAttribute("message", "탈퇴 요청이 정상적으로 처리되었습니다.");
      return "redirect:/member/processLogoutAfterAction"; // processLogout 페이지 거치고 안전하게 로그아웃
    } else {
      redirectAttributes.addFlashAttribute("message", "탈퇴 요청 처리에 실패했습니다.");
      return "redirect:/mypage";  // 실패 시 마이페이지로 이동
    }
  }

  private String createAuthCode() {
    Random random = new Random();
    return String.valueOf(100000 + random.nextInt(900000));
  }
}