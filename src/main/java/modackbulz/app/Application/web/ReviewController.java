package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.review.svc.ReviewSVC;
import modackbulz.app.Application.entity.Review;
import modackbulz.app.Application.web.form.login.LoginMember;
import modackbulz.app.Application.web.form.review.EditForm;
import modackbulz.app.Application.web.form.review.ReviewForm;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewSVC reviewSVC;
  private final GoCampingService goCampingService;

  // 리뷰 작성 폼 (GET 요청)
  @GetMapping("/write")
  public String writeForm(@RequestParam("campingId") Long campingId,
                          HttpSession session,
                          Model model,
                          RedirectAttributes redirectAttributes) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    // 1. 로그인 여부 확인
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "리뷰를 작성하려면 먼저 로그인해야 합니다.");
      return "redirect:/login";
    }

    // 2. 캠핑장 정보 조회
    GoCampingDto.Item camping = goCampingService.getCampDetail(campingId).block();
    if (camping == null) {
      redirectAttributes.addFlashAttribute("msg", "캠핑장 정보를 찾을 수 없습니다.");
      return "redirect:/camping";
    }

    // 3. 이미 작성한 리뷰가 있는지 확인
    List<Review> existingReviews = reviewSVC.findByContentId(campingId);
    Review existingReview = existingReviews.stream()
        .filter(review -> review.getMemberId().equals(loginMember.getMemberId()))
        .findFirst()
        .orElse(null);

    // 4. 평균 평점 계산
    double averageRating = 0.0;
    if (!existingReviews.isEmpty()) {
      double totalScore = existingReviews.stream()
          .mapToInt(Review::getScore)
          .sum();
      averageRating = Math.round((totalScore / existingReviews.size()) * 10.0) / 10.0;
    }

    // 5. 모델에 데이터 추가
    model.addAttribute("camping", camping);
    model.addAttribute("averageRating", averageRating);
    
    if (existingReview != null) {
      // 기존 리뷰가 있으면 수정 모드
      ReviewForm reviewForm = new ReviewForm();
      BeanUtils.copyProperties(existingReview, reviewForm);
      model.addAttribute("reviewForm", reviewForm);
      model.addAttribute("review", existingReview);
    } else {
      // 새 리뷰 작성 모드
      ReviewForm reviewForm = new ReviewForm();
      reviewForm.setCampingId(campingId);
      model.addAttribute("reviewForm", reviewForm);
    }

    return "review/review-form";
  }

  // 리뷰 작성/수정 처리 (POST 요청)
  @PostMapping
  public String write(@Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
                      BindingResult bindingResult,
                      HttpSession session,
                      RedirectAttributes redirectAttributes) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return "redirect:/login";
    }

    if (bindingResult.hasErrors()) {
      // 에러가 있으면 다시 작성 폼으로
      GoCampingDto.Item camping = goCampingService.getCampDetail(reviewForm.getCampingId()).block();
      if (camping != null) {
        redirectAttributes.addFlashAttribute("camping", camping);
      }
      return "review/review-form";
    }

    // 리뷰 엔티티 생성
    Review review = new Review();
    BeanUtils.copyProperties(reviewForm, review);
    review.setContentId(reviewForm.getCampingId()); // campingId -> contentId 매핑
    review.setMemberId(loginMember.getMemberId());
    review.setWriter(loginMember.getNickname());

    // 기존 리뷰가 있는지 확인
    List<Review> existingReviews = reviewSVC.findByContentId(reviewForm.getCampingId());
    Review existingReview = existingReviews.stream()
        .filter(r -> r.getMemberId().equals(loginMember.getMemberId()))
        .findFirst()
        .orElse(null);

    if (existingReview != null) {
      // 기존 리뷰 수정
      review.setRevId(existingReview.getRevId());
      reviewSVC.update(review);
      redirectAttributes.addFlashAttribute("msg", "리뷰가 성공적으로 수정되었습니다.");
    } else {
      // 새 리뷰 작성
      reviewSVC.save(review);
      redirectAttributes.addFlashAttribute("msg", "리뷰가 성공적으로 등록되었습니다.");
    }

    return "redirect:/camping/" + reviewForm.getCampingId();
  }

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

  // 리뷰 전체보기 페이지
  @GetMapping("/all")
  public String allReviews(@RequestParam("campingId") Long campingId, Model model) {
    // 캠핑장 정보
    GoCampingDto.Item camping = goCampingService.getCampDetail(campingId).block();
    model.addAttribute("camping", camping);

    // 리뷰 목록
    List<Review> reviews = reviewSVC.findByContentId(campingId);
    model.addAttribute("reviews", reviews);

    // 평점 분포 계산
    int[] counts = new int[6];
    for (Review r : reviews) counts[r.getScore()]++;
    int total = reviews.size();
    double avg = total > 0 ? reviews.stream().mapToInt(Review::getScore).average().orElse(0) : 0;
    model.addAttribute("averageRating", String.format("%.1f", avg));
    model.addAttribute("reviewCount", total);

    // 각 점수별 비율
    int[] percent = new int[6];
    for (int i = 1; i <= 5; i++) percent[i] = total > 0 ? (int) ((counts[i] * 100.0) / total) : 0;
    model.addAttribute("scorePercent", percent);

    // 키워드(임시)
    model.addAttribute("keywords", List.of("객실이 깨끗해요", "침구가 좋아요", "전망이 좋아요"));

    return "review/review-all";
  }
}