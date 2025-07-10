package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.review.svc.ReviewSVC;
import modackbulz.app.Application.entity.Review;
import modackbulz.app.Application.web.form.login.LoginMember;
import modackbulz.app.Application.web.form.review.EditForm; // 리뷰 수정용 폼 (새로 생성 필요)
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewSVC reviewSVC;

  // 리뷰 수정 폼 (GET 요청)
  @GetMapping("/{revId}/edit")
  public String editForm(@PathVariable("revId") Long revId,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    // 1. 로그인 여부 확인
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "리뷰를 수정하려면 먼저 로그인해야 합니다.");
      return "redirect:/login";
    }

    Review review = reviewSVC.findById(revId).orElse(null);
    // 2. 리뷰 존재 여부 확인
    if (review == null) {
      redirectAttributes.addFlashAttribute("msg", "해당 리뷰를 찾을 수 없습니다.");
      return "redirect:/"; // 혹은 캠핑장 상세 페이지로 리다이렉트
    }

    // 3. 권한 확인 (관리자 또는 작성자 본인)
    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = review.getMemberId().equals(loginMember.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "리뷰를 수정할 권한이 없습니다.");
      return "redirect:/camping/" + review.getContentId(); // 캠핑장 상세 페이지로 리다이렉트
    }

    // 뷰에 전달할 수정용 폼 객체 생성
    EditForm editForm = new EditForm();
    BeanUtils.copyProperties(review, editForm);
    editForm.setContentId(review.getContentId());

    model.addAttribute("editForm", editForm);
    model.addAttribute("revId", revId); // 폼 전송 시 사용할 리뷰 ID

    return "reviews/editForm"; // reviews/editForm.html (새로 생성 필요)
  }

  // 리뷰 수정 처리 (POST 요청)
  @PostMapping("/{revId}/edit")
  public String edit(@PathVariable("revId") Long revId,
                     @Valid @ModelAttribute("editForm") EditForm editForm,
                     BindingResult bindingResult,
                     HttpSession session,
                     RedirectAttributes redirectAttributes) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    Review review = reviewSVC.findById(revId).orElse(null);
    if (review == null) {
      return "redirect:/";
    }

    // 수정 권한 재확인
    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = review.getMemberId().equals(loginMember.getMemberId());
    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "수정 권한이 없습니다.");
      return "redirect:/camping/" + review.getContentId();
    }

    if (bindingResult.hasErrors()) {
      return "reviews/editForm";
    }

    // 수정 로직 수행
    BeanUtils.copyProperties(editForm, review);
    reviewSVC.update(review);

    return "redirect:/camping/" + review.getContentId(); // 수정 후 캠핑장 상세 페이지로 이동
  }


  // 리뷰 삭제 처리 (POST 요청)
  @PostMapping("/{revId}/delete")
  public String delete(@PathVariable("revId") Long revId,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    // 1. 로그인 여부 확인
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "리뷰를 삭제하려면 먼저 로그인해야 합니다.");
      return "redirect:/login";
    }

    Review review = reviewSVC.findById(revId).orElse(null);
    // 2. 리뷰 존재 여부 확인
    if (review == null) {
      redirectAttributes.addFlashAttribute("msg", "삭제할 리뷰를 찾을 수 없습니다.");
      return "redirect:/";
    }

    Long contentId = review.getContentId(); // 리다이렉션을 위해 미리 받아둠

    // 3. 권한 확인
    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = review.getMemberId().equals(loginMember.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "리뷰를 삭제할 권한이 없습니다.");
      return "redirect:/camping/" + contentId;
    }

    // 삭제 로직 수행
    reviewSVC.delete(revId);
    redirectAttributes.addFlashAttribute("msg", "리뷰가 성공적으로 삭제되었습니다.");

    return "redirect:/camping/" + contentId; // 삭제 후 캠핑장 상세 페이지로 이동
  }
}