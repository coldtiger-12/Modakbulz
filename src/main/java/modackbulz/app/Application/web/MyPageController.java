package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.web.form.login.LoginMember;
import modackbulz.app.Application.web.form.member.EditForm;
import modackbulz.app.Application.web.form.member.EditForm_Pwd;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

  private final MemberSVC memberSVC;

  // 마이페이지 메인 화면
  @GetMapping
  public String myPage(HttpSession session) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }
    return "member/mypage";
  }

  // 회원정보 수정 폼
  @GetMapping("/edit")
  public String editForm(HttpSession session, Model model) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    Member member = memberSVC.findById(loginMember.getId())
        .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

    EditForm editForm = new EditForm();
    editForm.setNickname(member.getNickname());
    editForm.setTel(member.getTel());
    editForm.setRegion(member.getRegion());
    editForm.setEmail(member.getEmail());

    model.addAttribute("editForm", editForm);
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

    if (bindingResult.hasErrors()) {
      return "member/editForm";
    }

    Member member = new Member();
    member.setMemberId(loginMember.getMemberId());
    member.setNickname(editForm.getNickname());
    member.setTel(editForm.getTel());
    member.setRegion(editForm.getRegion());

    boolean updated = memberSVC.updateMember(member);
    if (!updated) {
      bindingResult.reject("updateFail", "회원정보 수정에 실패했습니다.");
      return "member/editForm";
    }
    return "redirect:/mypage?success";
  }

  // 비밀번호 변경 폼
  @GetMapping("/password")
  public String changePasswordForm(Model model, HttpSession session) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }
    model.addAttribute("editForm_Pwd", new EditForm_Pwd());
    return "member/editForm_pwd";
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