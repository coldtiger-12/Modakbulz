package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.scrap.svc.CampScrapService;
import modackbulz.app.Application.entity.CampScrap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
public class CampScrapApiController {

  private final CampScrapService campScrapService;

  @PostMapping("/toggle")
  public ResponseEntity<Map<String, Object>> toggleScrap(@RequestBody CampScrap scrapRequest, @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
    }

    scrapRequest.setMemberId(userDetails.getMemberId());

    campScrapService.toggleScrap(scrapRequest);

    boolean isScrapped = campScrapService.isScrapped(userDetails.getMemberId(), scrapRequest.getContentId());

    return ResponseEntity.ok(Map.of("message", "Success", "isScrapped", isScrapped));
  }
}