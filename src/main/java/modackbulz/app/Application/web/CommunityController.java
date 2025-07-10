package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.community.svc.CommunitySVC;
import modackbulz.app.Application.entity.Community;
import modackbulz.app.Application.web.form.community.EditForm;
import modackbulz.app.Application.web.form.community.SaveForm;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/posts/community")
@RequiredArgsConstructor
public class CommunityController {

  private final CommunitySVC communityService;

  // 게시글 목록 조회
  @GetMapping
  public String listPage(Model model) {
    List<Community> posts = communityService.getAllPosts();
    model.addAttribute("posts", posts);
    return "posts/community/list";
  }

  // 게시글 등록
  @GetMapping("/new")
  public String saveForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "회원만 글 작성 가능합니다.");
      return "redirect:/posts/community";
    }

    SaveForm form = new SaveForm();
    form.setWriter(userDetails.getNickname());
    model.addAttribute("communityForm", form);
    return "posts/community/saveForm";
  }

  // 게시글 등록 처리
  @PostMapping("/save")
  public String savePost(@Valid @ModelAttribute("communityForm") SaveForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         RedirectAttributes redirectAttributes) {

    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }
    if (bindingResult.hasErrors()) {
      return "posts/community/saveForm";
    }

    Community community = new Community();
    BeanUtils.copyProperties(form, community);

    // userDetails에서 직접 memberId와 nickname 설정
    community.setMemberId(userDetails.getMemberId());
    community.setWriter(userDetails.getNickname());

    communityService.createPost(community);
    return "redirect:/posts/community";
  }

  // 게시글 상세 조회 + 조회수 증가
  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, Model model) {
    communityService.increaseViewCount(id);
    Community post = communityService.getPostById(id).orElse(null);
    if (post == null) {
      return "redirect:/posts/community";
    }
    model.addAttribute("post", post);
    return "posts/community/detail";
  }

  // 게시글 수정 폼
  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable("id") Long id,
                         @AuthenticationPrincipal CustomUserDetails userDetails,
                         Model model,
                         RedirectAttributes redirectAttributes) {
    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }

    Community post = communityService.getPostById(id).orElse(null);

    if (post == null) {
      redirectAttributes.addFlashAttribute("msg", "해당 게시글을 찾을 수 없습니다.");
      return "redirect:/posts/community";
    }

    boolean isAdmin = "A".equals(userDetails.getGubun());
    boolean isOwner = post.getMemberId().equals(userDetails.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "수정 권한이 없습니다.");
      return "redirect:/posts/community/" + id;
    }

    EditForm form = new EditForm();
    BeanUtils.copyProperties(post, form);
    model.addAttribute("communityForm", form);
    return "posts/community/editForm";
  }


  // 게시글 수정 처리
  @PostMapping("/{id}/edit")
  public String updatePost(@PathVariable("id") Long id,
                           @Valid @ModelAttribute("communityForm") EditForm form,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           RedirectAttributes redirectAttributes) {

    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }

    Community post = communityService.getPostById(id).orElse(null);
    if (post == null) {
      redirectAttributes.addFlashAttribute("msg", "해당 게시글을 찾을 수 없습니다.");
      return "redirect:/posts/community";
    }

    boolean isAdmin = "A".equals(userDetails.getGubun());
    boolean isOwner = post.getMemberId().equals(userDetails.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "수정 권한이 없습니다.");
      return "redirect:/posts/community/" + id;
    }

    if (bindingResult.hasErrors()) {
      return "posts/community/editForm";
    }

    Community community = new Community();
    BeanUtils.copyProperties(form, community);
    community.setCoId(id);

    communityService.updatePost(community);
    return "redirect:/posts/community/" + id;
  }


  // 게시글 삭제 처리
  @PostMapping("/{id}/delete")
  public String deletePost(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
    if (userDetails == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }

    Community post = communityService.getPostById(id).orElse(null);
    if (post == null) {
      redirectAttributes.addFlashAttribute("msg", "삭제할 게시글을 찾을 수 없습니다.");
      return "redirect:/posts/community";
    }

    boolean isAdmin = "A".equals(userDetails.getGubun());
    boolean isOwner = post.getMemberId().equals(userDetails.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "삭제 권한이 없습니다.");
      return "redirect:/posts/community";
    }

    communityService.deletePost(id);
    redirectAttributes.addFlashAttribute("msg", id + "번 게시글이 삭제되었습니다.");
    return "redirect:/posts/community";
  }
}