package modackbulz.app.Application.web;

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

import jakarta.servlet.http.HttpSession;
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
  private final PasswordEncoder passwordEncoder;

  // 회원정보 수정 폼
  @GetMapping({"", "/edit"})
  public String editForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
    if (userDetails == null) {
      return "redirect:/login";
    }

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
      RedirectAttributes redirectAttributes) {

    if (userDetails == null) return "redirect:/login";
    if (bindingResult.hasErrors()) return "member/editForm_pwd";

    Optional<Member> memberOptional = memberSVC.findById(userDetails.getUsername());
    if(memberOptional.isEmpty()) return "redirect:/login";

    Member member = memberOptional.get();
    boolean changed = memberSVC.changePassword(member.getMemberId(), passwordEncoder.encode(editForm_Pwd.getPwd()));

    if (changed) {
      redirectAttributes.addFlashAttribute("message", "비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.");
      return "redirect:/login"; // 비밀번호 변경 후 보안을 위해 로그아웃 처리 후 로그인 페이지로 이동
    } else {
      bindingResult.reject("changePwdFail", "비밀번호 변경에 실패했습니다.");
      return "member/editForm_pwd";
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

    return "redirect:/logout"; // 탈퇴 후 로그아웃 처리
  }
}