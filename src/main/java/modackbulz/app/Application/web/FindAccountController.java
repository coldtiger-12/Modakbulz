package modackbulz.app.Application.web;


import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.member.svc.MemberSVC;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/find")
public class FindAccountController {

  private final MemberSVC memberSVC;

  // 아이디-비밀번호 찾기 메인 페이지
  @GetMapping
  public String findAccountForm(){
    return "login/findAccountForm";
  }

  // 아이디 찾기 결과
  @PostMapping("/id")
  public String findId(@RequestParam("email") String email, RedirectAttributes redirectAttributes){
    Optional<String> foundId = memberSVC.findIdByEmail(email);
    if (foundId.isPresent()){
      redirectAttributes.addFlashAttribute("message", "회원님의 아이디는 [ " + foundId.get() + " ] 입니다");
    } else {
      redirectAttributes.addFlashAttribute("message", "해당 이메일로 가입된 아이디가 없습니다.");
    }

    return "redirect:/find/result";
  }

  // 비밀번호 재설정
  @PostMapping("/password")
  public String findPassword(
      @RequestParam("id") String id,
      @RequestParam("email") String email,
      RedirectAttributes redirectAttributes
      ){
    boolean result = memberSVC.issueTempPassword(id, email);
    if (result){
      redirectAttributes.addFlashAttribute("message", "가입하신 이메일로 임시 비밀번호가 발송되었습니다.");
    } else {
      redirectAttributes.addFlashAttribute("message", "입력하신 정보와 일치하는 회원이 없습니다.");
    }
    return "redirect:/find/result";
  }

  // 결과 페이지만을 보여주는 새로운 get 메소드
  @GetMapping("/result")
  public String showResult(Model model){
    // RedirectAttributes로 전달된 메시지는 자동으로 Model에 추가됨
    // 만약 message 속성이 없다면 비정상적인 접근으로 간주할 수 있음
    if (!model.containsAttribute("message")){
      // 비정상 접근 시 에러 메시지를 설정하거나 로그인 페이지로 보낼수가 있음
      return "redirect:/login";
    }
    return "login/findResult";
  }
}
