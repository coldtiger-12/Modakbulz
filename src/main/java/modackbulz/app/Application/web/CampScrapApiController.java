package modackbulz.app.Application.web;


import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.config.auth.CustomUserDetails;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.scrap.dao.CampScrapDAO;
import modackbulz.app.Application.entity.CampScrap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
public class CampScrapApiController {

  private final CampScrapDAO campScrapDAO;

  @PostMapping("/toggle")
  public ResponseEntity<?> toggleScrap(@RequestBody GoCampingDto.Item campItem,
                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
    if (userDetails == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    Long memberId = userDetails.getMemberId();
    Long contentId = campItem.getContentId();

    Optional<CampScrap> existing = campScrapDAO.findByMemberIdAndContentId(memberId, contentId);

    boolean isScrapped;

    if (existing.isPresent()) {
      campScrapDAO.delete(memberId, contentId);
      isScrapped = false;
    } else {
      CampScrap newScrap = CampScrap.builder()
          .memberId(memberId)
          .contentId(contentId)
          .facltNm(campItem.getFacltNm())
          .addr1(campItem.getAddr1())
          .firstImageUrl(campItem.getFirstImageUrl())
          .build();
      campScrapDAO.add(newScrap);
      isScrapped = true;
    }

    return ResponseEntity.ok(Map.of("isScrapped", isScrapped));
  }
}