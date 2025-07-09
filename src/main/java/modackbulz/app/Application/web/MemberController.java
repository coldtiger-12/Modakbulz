package modackbulz.app.Application.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.web.form.member.JoinForm;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
  private final MemberSVC memberSVC;

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
      Model model
  ) {
    log.info("회원가입 요청: {}", joinForm);

    // 1. 유효성 검사 실패 시 다시 폼으로
    if (bindingResult.hasErrors()) {
      return "member/joinForm";
    }

    // 2. 아이디 중복 체크
    if (memberSVC.isExist(joinForm.getId())) {
      bindingResult.rejectValue("id", "duplicate", "이미 등록된 아이디입니다.");
      return "member/joinForm";
    }

    // 3. JoinForm → Member 매핑
    Member member = new Member();
    BeanUtils.copyProperties(joinForm, member);

    // 4. 기본값 설정
    member.setPwd(joinForm.getPwd());    // 암호화 없이 그대로 저장
    member.setIsDel(null);
    member.setDelDate(null);
    member.setGubun("U");

    try {
      memberSVC.insertMember(member);
    } catch (Exception e) {
      log.error("회원가입 실패", e);
      bindingResult.reject("joinFail", "회원가입 처리 중 오류가 발생했습니다.");
      return "member/joinForm";
    }

    // 5. 회원가입 성공 → 로그인 페이지로 이동
    return "redirect:/login";
  }
}