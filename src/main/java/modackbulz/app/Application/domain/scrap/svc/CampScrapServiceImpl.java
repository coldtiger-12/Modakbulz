package modackbulz.app.Application.domain.scrap.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.scrap.dao.CampScrapDAO;
import modackbulz.app.Application.entity.CampScrap;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampScrapServiceImpl implements CampScrapService {

  private final CampScrapDAO campScrapDAO;

  @Override
  public boolean toggleScrap(CampScrap campScrap) {
    // 이미 스크랩되어 있다면 삭제, 아니면 추가
    if (isScrapped(campScrap.getMemberId(), campScrap.getContentId())) {
      return campScrapDAO.delete(campScrap.getMemberId(), campScrap.getContentId()) > 0;
    } else {
      campScrapDAO.add(campScrap);
      return true;
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