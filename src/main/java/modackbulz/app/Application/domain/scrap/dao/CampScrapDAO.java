package modackbulz.app.Application.domain.scrap.dao;

import modackbulz.app.Application.entity.CampScrap;
import java.util.List;
import java.util.Optional;

public interface CampScrapDAO {
  // 스크랩 추가
  CampScrap add(CampScrap campScrap);

  // 스크랩 삭제
  int delete(Long memberId, Long contentId);

  // 특정 회원의 스크랩 목록 조회
  List<CampScrap> findByMemberId(Long memberId);

  // 특정 회원이 특정 캠핑장을 스크랩했는지 확인
  Optional<CampScrap> findByMemberIdAndContentId(Long memberId, Long contentId);
}