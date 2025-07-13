package modackbulz.app.Application.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.community.svc.CommunitySVC;
import modackbulz.app.Application.domain.community.svc.CoCommentSVC;
import modackbulz.app.Application.entity.CoComment;
import modackbulz.app.Application.entity.Community;
import modackbulz.app.Application.web.form.community.CoCommentForm;
import modackbulz.app.Application.web.form.community.EditForm;
import modackbulz.app.Application.web.form.community.SaveForm;
import modackbulz.app.Application.web.form.login.LoginMember;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/posts/community")
@RequiredArgsConstructor
public class CommunityController {

  private final CommunitySVC communityService;
  private final CoCommentSVC coCommentService;

  // 게시글 목록 조회
  @GetMapping
  public String listPage(Model model) {
    List<Community> posts = communityService.getAllPosts();
    model.addAttribute("posts", posts);
    return "posts/community/list";
  }

  // 게시글 등록 폼
  @GetMapping("/new")
  public String saveForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");

    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "회원만 글 작성 가능합니다.");
      return "redirect:/posts/community";
    }

    SaveForm form = new SaveForm();
    form.setWriter(loginMember.getNickname());
    model.addAttribute("communityForm", form);
    return "posts/community/saveForm";
  }

  // 게시글 등록 처리
  @PostMapping("/save")
  public String savePost(@Valid @ModelAttribute("communityForm") SaveForm form,
                         BindingResult bindingResult,
                         HttpSession session) {
    if (bindingResult.hasErrors()) {
      return "posts/community/saveForm";
    }

    Community community = new Community();
    BeanUtils.copyProperties(form, community);

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember != null) {
      community.setMemberId(loginMember.getMemberId());
      community.setWriter(loginMember.getNickname());
    }

    communityService.createPost(community);
    return "redirect:/posts/community";
  }

  // 게시글 상세 조회 + 조회수 증가 + 댓글 목록 조회 + 댓글 작성 폼
  @GetMapping("/{id}")
  public String detail(@PathVariable("id") Long id, Model model, HttpSession session) {
    communityService.increaseViewCount(id);
    Community post = communityService.getPostById(id).orElse(null);
    if (post == null) {
      return "redirect:/posts/community";
    }

    List<CoComment> comments = new ArrayList<>(coCommentService.getCommentsByPostId(id));
    comments.removeIf(Objects::isNull);
    model.addAttribute("comments", comments);

    model.addAttribute("post", post);
    model.addAttribute("comments", comments);

    CoCommentForm commentForm = new CoCommentForm();
    model.addAttribute("commentForm", commentForm);

    CoCommentForm replyForm = new CoCommentForm();
    model.addAttribute("replyForm", replyForm);

    model.addAttribute("isReplying", false);

    return "posts/community/detail";
  }

  // 게시글 수정 폼
  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable("id") Long id,
                         HttpSession session,
                         Model model,
                         RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");

    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }

    Community post = communityService.getPostById(id).orElse(null);

    if (post == null) {
      redirectAttributes.addFlashAttribute("msg", "해당 게시글을 찾을 수 없습니다.");
      return "redirect:/posts/community";
    }

    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = post.getMemberId().equals(loginMember.getMemberId());

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
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    Community post = communityService.getPostById(id).orElse(null);

    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }
    if (post == null) {
      redirectAttributes.addFlashAttribute("msg", "해당 게시글을 찾을 수 없습니다.");
      return "redirect:/posts/community";
    }

    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = post.getMemberId().equals(loginMember.getMemberId());

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
  public String deletePost(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    Community post = communityService.getPostById(id).orElse(null);

    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/login";
    }
    if (post == null) {
      redirectAttributes.addFlashAttribute("msg", "삭제할 게시글을 찾을 수 없습니다.");
      return "redirect:/posts/community";
    }

    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = post.getMemberId().equals(loginMember.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "삭제 권한이 없습니다.");
      return "redirect:/posts/community";
    }

    communityService.deletePost(id);
    redirectAttributes.addFlashAttribute("msg", id + "번 게시글이 삭제되었습니다.");
    return "redirect:/posts/community";
  }


  // --- 댓글 기능 ---

  // 댓글 등록 처리
  @PostMapping("/{postId}/comments/save")
  public String saveComment(@PathVariable("postId") Long postId,
                            @Valid @ModelAttribute("commentForm") CoCommentForm form,
                            BindingResult bindingResult,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/posts/community/" + postId;
    }

    if (bindingResult.hasErrors()) {
      Community post = communityService.getPostById(postId).orElse(null);
      List<CoComment> comments = coCommentService.getCommentsByPostId(postId);
      model.addAttribute("post", post);
      model.addAttribute("comments", comments);

      // 추가
      model.addAttribute("replyForm", new CoCommentForm());
      model.addAttribute("isReplying", false);

      return "posts/community/detail";
    }

    CoComment comment = new CoComment();
    comment.setCoId(postId);
    comment.setMemberId(loginMember.getMemberId());
    comment.setWriter(loginMember.getNickname());
    comment.setContent(form.getContent());
    comment.setPrcComId(form.getPrcComId());

    coCommentService.createComment(comment);
    redirectAttributes.addFlashAttribute("msg", "댓글이 등록되었습니다.");
    return "redirect:/posts/community/" + postId;
  }


  // 댓글 수정 폼
  @GetMapping("/{postId}/comments/{commentId}/edit")
  public String editCommentForm(@PathVariable("postId") Long postId,
                                @PathVariable("commentId") Long commentId,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/posts/community/" + postId;
    }

    CoComment comment = coCommentService.getCommentById(commentId).orElse(null);
    if (comment == null) {
      redirectAttributes.addFlashAttribute("msg", "댓글을 찾을 수 없습니다.");
      return "redirect:/posts/community/" + postId;
    }

    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = comment.getMemberId().equals(loginMember.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "수정 권한이 없습니다.");
      return "redirect:/posts/community/" + postId;
    }

    Community post = communityService.getPostById(postId).orElse(null);
    List<CoComment> comments = coCommentService.getCommentsByPostId(postId);

    CoCommentForm form = new CoCommentForm();
    BeanUtils.copyProperties(comment, form);

    model.addAttribute("post", post);
    model.addAttribute("comments", comments);
    model.addAttribute("commentForm", form);
    model.addAttribute("editCommentId", commentId);

    // 추가
    model.addAttribute("replyForm", new CoCommentForm());
    model.addAttribute("isReplying", false);

    return "posts/community/detail";
  }

  // 댓글 수정 처리
  @PostMapping("/{postId}/comments/{commentId}/edit")
  public String updateComment(@PathVariable("postId") Long postId,
                              @PathVariable("commentId") Long commentId,
                              @Valid @ModelAttribute("commentForm") CoCommentForm form,
                              BindingResult bindingResult,
                              HttpSession session,
                              RedirectAttributes redirectAttributes,
                              Model model) {

    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/posts/community/" + postId;
    }

    CoComment original = coCommentService.getCommentById(commentId).orElse(null);
    if (original == null) {
      redirectAttributes.addFlashAttribute("msg", "댓글을 찾을 수 없습니다.");
      return "redirect:/posts/community/" + postId;
    }

    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = original.getMemberId().equals(loginMember.getMemberId());
    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "수정 권한이 없습니다.");
      return "redirect:/posts/community/" + postId;
    }

    if (bindingResult.hasErrors()) {
      Community post = communityService.getPostById(postId).orElse(null);
      List<CoComment> comments = coCommentService.getCommentsByPostId(postId);
      model.addAttribute("post", post);
      model.addAttribute("comments", comments);
      model.addAttribute("editCommentId", commentId);

      // 추가
      model.addAttribute("replyForm", new CoCommentForm());
      model.addAttribute("isReplying", false);

      return "posts/community/detail";
    }

    CoComment updated = new CoComment();
    updated.setCComId(commentId);
    updated.setCoId(postId);
    updated.setMemberId(original.getMemberId());
    updated.setWriter(original.getWriter());
    updated.setContent(form.getContent());
    updated.setPrcComId(original.getPrcComId());

    coCommentService.updateComment(updated);
    redirectAttributes.addFlashAttribute("msg", "댓글이 수정되었습니다.");
    return "redirect:/posts/community/" + postId;
  }

  // 댓글 삭제 처리
  @PostMapping("/{postId}/comments/{commentId}/delete")
  public String deleteComment(@PathVariable("postId") Long postId,
                              @PathVariable("commentId") Long commentId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/posts/community/" + postId;
    }

    CoComment comment = coCommentService.getCommentById(commentId).orElse(null);
    if (comment == null) {
      redirectAttributes.addFlashAttribute("msg", "댓글을 찾을 수 없습니다.");
      return "redirect:/posts/community/" + postId;
    }

    boolean isAdmin = "A".equals(loginMember.getGubun());
    boolean isOwner = comment.getMemberId().equals(loginMember.getMemberId());

    if (!isAdmin && !isOwner) {
      redirectAttributes.addFlashAttribute("msg", "삭제 권한이 없습니다.");
      return "redirect:/posts/community/" + postId;
    }

    coCommentService.deleteComment(commentId);
    redirectAttributes.addFlashAttribute("msg", "댓글이 삭제되었습니다.");
    return "redirect:/posts/community/" + postId;
  }

  // 대댓글 작성 폼 (필요하면 만듦)
  // 대댓글 작성 폼 (답글 작성 모드 진입)
  @GetMapping("/{postId}/comments/{parentCommentId}/reply")
  public String replyCommentForm(@PathVariable("postId") Long postId,
                                 @PathVariable("parentCommentId") Long parentCommentId,
                                 Model model,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      redirectAttributes.addFlashAttribute("msg", "로그인이 필요합니다.");
      return "redirect:/posts/community/" + postId;
    }

    Community post = communityService.getPostById(postId).orElse(null);
    if (post == null) {
      redirectAttributes.addFlashAttribute("msg", "게시글을 찾을 수 없습니다.");
      return "redirect:/posts/community";
    }

    model.addAttribute("post", post);
    model.addAttribute("comments", coCommentService.getCommentsByPostId(postId));

    CoCommentForm replyForm = new CoCommentForm();
    replyForm.setPrcComId(parentCommentId);
    model.addAttribute("replyForm", replyForm);

    model.addAttribute("isReplying", true);

    model.addAttribute("commentForm", new CoCommentForm());
    model.addAttribute("editCommentId", null); // 수정 모드는 아니므로 null

    return "posts/community/detail";
  }
}