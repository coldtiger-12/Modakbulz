package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.common.FileStore;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import modackbulz.app.Application.domain.keyword.svc.KeywordSVC;
import modackbulz.app.Application.domain.review.svc.ReviewSVC;
import modackbulz.app.Application.entity.Review;
import modackbulz.app.Application.entity.UploadFile;
import modackbulz.app.Application.web.form.login.LoginMember;
import modackbulz.app.Application.web.form.review.EditForm;
import modackbulz.app.Application.web.form.review.ReviewForm;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

  //== 의존성 주입 ==//
  private final ReviewSVC reviewSVC;
  private final GoCampingService goCampingService;
  private final KeywordSVC keywordSVC;
  private final FileStore fileStore;

  /**
   * 리뷰 작성 폼 (GET) - 수정된 코드
   */
  @GetMapping("/write")
  public String writeForm(@RequestParam("campingId") Long campingId, Model model,
                          @AuthenticationPrincipal CustomUserDetails userDetails, // HttpSession 대신 사용
                          RedirectAttributes redirectAttributes) {

    // 1. Spring Security를 통해 로그인 정보 확인
    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "리뷰를 작성하려면 먼저 로그인해야 합니다.");
      return "redirect:/login";
    }

    model.addAttribute("reviewForm", new ReviewForm());
    model.addAttribute("camping", goCampingService.getCampDetail(campingId).block());
    model.addAttribute("allKeywords", keywordSVC.findAll());

    double averageRating = reviewSVC.calculateAverageScore(campingId);
    model.addAttribute("averageRating", averageRating);

    return "review/review-form";
  }

  /**
   * 새 리뷰 작성 처리 (POST) - 수정된 코드
   */
  @PostMapping("/write")
  public String write(@Valid @ModelAttribute("reviewForm") ReviewForm reviewForm,
                      BindingResult bindingResult,
                      @AuthenticationPrincipal CustomUserDetails userDetails, // HttpSession 대신 사용
                      RedirectAttributes redirectAttributes,
                      Model model) throws IOException {

    if (userDetails == null) {
      return "redirect:/login";
    }

    if (bindingResult.hasErrors()) {
      log.info("리뷰 작성 유효성 검사 오류: {}", bindingResult);
      model.addAttribute("camping", goCampingService.getCampDetail(reviewForm.getCampingId()).block());
      model.addAttribute("allKeywords", keywordSVC.findAll());
      return "review/review-form";
    }

    List<UploadFile> storedFiles = fileStore.storeFiles(reviewForm.getImageFiles());

    Review review = new Review();
    BeanUtils.copyProperties(reviewForm, review);
    review.setContentId(reviewForm.getCampingId());
    // 2. userDetails 객체에서 회원 정보 가져오기
    review.setMemberId(userDetails.getMemberId());
    review.setWriter(userDetails.getNickname());
    review.setKeywordIds(reviewForm.getKeywordIds());
    review.setFiles(storedFiles);

    reviewSVC.save(review);
    redirectAttributes.addFlashAttribute("msg", "리뷰가 성공적으로 등록되었습니다.");

    return "redirect:/camping/" + reviewForm.getCampingId();
  }

  /**
   * 리뷰 수정 폼 (GET) - 수정된 코드
   */
  @GetMapping("/{revId}/edit")
  public String editForm(@PathVariable("revId") Long revId, Model model,
                         @AuthenticationPrincipal CustomUserDetails userDetails, // HttpSession 대신 사용
                         RedirectAttributes redirectAttributes) {
    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }

    Optional<Review> optionalReview = reviewSVC.findById(revId);
    if (optionalReview.isEmpty()) {
      redirectAttributes.addFlashAttribute("msg", "존재하지 않는 리뷰입니다.");
      return "redirect:/";
    }

    Review review = optionalReview.get();
    // userDetails에서 직접 memberId를 가져와 비교합니다.
    if (!review.getMemberId().equals(userDetails.getMemberId())) {
      redirectAttributes.addFlashAttribute("msg", "수정 권한이 없습니다.");
      return "redirect:/camping/" + review.getContentId();
    }

    EditForm editForm = new EditForm();
    BeanUtils.copyProperties(review, editForm);
    editForm.setKeywordIds(review.getKeywordIds());

    model.addAttribute("editForm", editForm);
    model.addAttribute("revId", revId);
    model.addAttribute("allKeywords", keywordSVC.findAll());
    model.addAttribute("existingFiles", review.getFiles());

    return "review/review-edit-form";
  }

  /**
   * 리뷰 수정 처리 (POST) - 수정된 코드
   */
  @PostMapping("/{revId}/edit")
  public String edit(@PathVariable("revId") Long revId,
                     @Valid @ModelAttribute("editForm") EditForm editForm,
                     BindingResult bindingResult,
                     @AuthenticationPrincipal CustomUserDetails userDetails, // HttpSession 대신 사용
                     RedirectAttributes redirectAttributes,
                     Model model) throws IOException {

    if (userDetails == null) return "redirect:/login";

    Optional<Review> optionalReview = reviewSVC.findById(revId);
    if (optionalReview.isEmpty()) {
      redirectAttributes.addFlashAttribute("msg", "존재하지 않는 리뷰입니다.");
      return "redirect:/";
    }

    Review foundReview = optionalReview.get();
    if (!foundReview.getMemberId().equals(userDetails.getMemberId())) {
      redirectAttributes.addFlashAttribute("msg", "수정 권한이 없습니다.");
      return "redirect:/camping/" + foundReview.getContentId();
    }
    // ... (이하 로직은 동일)
    if (bindingResult.hasErrors()) {
      log.info("리뷰 수정 유효성 검사 오류: {}", bindingResult);
      model.addAttribute("revId", revId);
      model.addAttribute("allKeywords", keywordSVC.findAll());
      model.addAttribute("existingFiles", foundReview.getFiles());
      return "review/review-edit-form";
    }

    // 1. 물리적 파일 삭제 (DB 삭제보다 먼저 수행)
    List<Long> deletedFileIds = editForm.getDeletedFileIds();
    if (deletedFileIds != null && !deletedFileIds.isEmpty()) {
      List<UploadFile> allFiles = foundReview.getFiles();
      List<UploadFile> filesToDelete = allFiles.stream()
          .filter(file -> deletedFileIds.contains(file.getFileId()))
          .toList();
      fileStore.deleteFiles(filesToDelete);
    }

    // 2. 새로 업로드된 파일 서버에 저장
    List<UploadFile> newStoredFiles = fileStore.storeFiles(editForm.getNewImageFiles());

    // 3. 서비스 계층에 전달할 Review 객체 준비
    Review reviewToUpdate = new Review();
    BeanUtils.copyProperties(editForm, reviewToUpdate);
    reviewToUpdate.setRevId(revId);
    reviewToUpdate.setFiles(newStoredFiles);

    // 4. 서비스 호출하여 DB 업데이트
    reviewSVC.update(reviewToUpdate, deletedFileIds);
    redirectAttributes.addFlashAttribute("msg", "리뷰가 성공적으로 수정되었습니다.");

    return "redirect:/camping/" + editForm.getContentId();
  }


  /**
   * 리뷰 삭제 처리 (POST) - 수정된 코드
   */
  @PostMapping("/{revId}/delete")
  public String delete(@PathVariable("revId") Long revId,
                       @AuthenticationPrincipal CustomUserDetails userDetails, // HttpSession 대신 사용
                       RedirectAttributes redirectAttributes) {
    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }

    Optional<Review> optionalReview = reviewSVC.findById(revId);
    if (optionalReview.isEmpty()) {
      redirectAttributes.addFlashAttribute("msg", "삭제할 리뷰를 찾을 수 없습니다.");
      return "redirect:/";
    }

    Review review = optionalReview.get();
    Long contentId = review.getContentId();
    boolean isAdmin = "A".equals(userDetails.getGubun()); // userDetails에서 gubun 정보를 가져옵니다.
    boolean isOwner = review.getMemberId().equals(userDetails.getMemberId()); // userDetails에서 memberId를 가져옵니다.

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "리뷰를 삭제할 권한이 없습니다.");
      return "redirect:/camping/" + contentId;
    }

    reviewSVC.delete(revId);
    redirectAttributes.addFlashAttribute("msg", "리뷰가 성공적으로 삭제되었습니다.");

    return "redirect:/camping/" + contentId;
  }

  /**
   * 리뷰 전체보기 페이지 (GET)
   */
  @GetMapping("/all")
  public String allReviews(@RequestParam("campingId") Long campingId, Model model) {
    GoCampingDto.Item camping = goCampingService.getCampDetail(campingId).block();
    model.addAttribute("camping", camping);

    List<Review> reviews = reviewSVC.findByContentId(campingId);
    model.addAttribute("reviews", reviews);

    int total = reviews.size();
    double avg = reviewSVC.calculateAverageScore(campingId);
    model.addAttribute("averageRating", String.format("%.1f", avg));
    model.addAttribute("reviewCount", total);

    Map<Integer, Long> scoreDistribution = reviewSVC.calculateScoreDistribution(campingId);
    int[] percent = new int[6];
    for (int i = 1; i <= 5; i++) {
      long count = scoreDistribution.getOrDefault(i, 0L);
      percent[i] = total > 0 ? (int) ((count * 100.0) / total) : 0;
    }
    model.addAttribute("scorePercent", percent);

    // (참고) 실제 서비스에서는 리뷰에 포함된 키워드들을 집계하는 로직 필요
    model.addAttribute("keywords", List.of("힐링", "자연친화", "휴양지", "봄꽃", "여름나기", "가을낙엽", "겨울눈구경"));

    return "review/review-all";
  }
}