package modackbulz.app.Application.domain.scrap.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.scrap.dao.CampScrapDAO;
import modackbulz.app.Application.entity.CampScrap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampScrapServiceImpl implements CampScrapService {

  private final CampScrapDAO campScrapDAO;

  @Override
  @Transactional
  public boolean toggleScrap(CampScrap campScrap) {
    // 컨트롤러에서 넘어온 memberId와 contentId를 변수로 추출
    Long memberId = campScrap.getMemberId();
    Long contentId = campScrap.getContentId();

    // isScrapped 메소드 대신, 직접 DB에서 조회하여 확인합니다. (트랜잭션 내 일관성 유지)
    if (campScrapDAO.findByMemberIdAndContentId(memberId, contentId).isPresent()) {
      // 1. 스크랩이 이미 존재하면 -> 삭제
      campScrapDAO.delete(memberId, contentId);
      // 2. [핵심] 총 스크랩 수 -1 업데이트
      campScrapDAO.updateScrapCount(contentId, -1);
      return false; // 스크랩 취소됨을 반환
    } else {
      // 1. 스크랩이 없으면 -> 추가
      campScrapDAO.add(campScrap);
      // 2. [핵심] 총 스크랩 수 +1 업데이트
      campScrapDAO.updateScrapCount(contentId, 1);
      return true; // 스크랩 추가됨을 반환
    }
  }

  @Override
  public List<CampScrap> getMyScraps(Long memberId) {
    return campScrapDAO.findByMemberId(memberId);
  }

  @Override
  public boolean isScrapped(Long memberId, Long contentId) {
    return campScrapDAO.findByMemberIdAndContentId(memberId, contentId).isPresent();
  }
}