package modackbulz.app.Application.domain.scrap.svc;

import modackbulz.app.Application.entity.CampScrap;
import java.util.List;

public interface CampScrapService {
  // 스크랩 토글 (추가/삭제)
  boolean toggleScrap(CampScrap campScrap);

  // 내 스크랩 목록 보기
  List<CampScrap> getMyScraps(Long memberId);

  // 스크랩 상태 확인
  boolean isScrapped(Long memberId, Long contentId);
}