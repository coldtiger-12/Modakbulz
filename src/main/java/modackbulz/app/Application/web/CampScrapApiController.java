package modackbulz.app.Application.web;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.scrap.svc.CampScrapService;
import modackbulz.app.Application.entity.CampScrap;
import modackbulz.app.Application.web.form.login.LoginMember;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
public class CampScrapApiController {

  private final CampScrapService campScrapService;

  @PostMapping("/toggle")
  public ResponseEntity<Map<String, Object>> toggleScrap(@RequestBody CampScrap scrapRequest, HttpSession session) {
    LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
    if (loginMember == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
    }

    scrapRequest.setMemberId(loginMember.getMemberId());

    campScrapService.toggleScrap(scrapRequest);

    // 토글 후 현재 스크랩 상태를 다시 확인하여 반환
    boolean isScrapped = campScrapService.isScrapped(loginMember.getMemberId(), scrapRequest.getContentId());

    return ResponseEntity.ok(Map.of("message", "Success", "isScrapped", isScrapped));
  }
}