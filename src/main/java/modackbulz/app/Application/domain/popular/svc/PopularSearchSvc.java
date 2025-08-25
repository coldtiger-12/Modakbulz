package modackbulz.app.Application.domain.popular.svc;

import modackbulz.app.Application.domain.popular.dto.PopularSearchDto;

import java.util.List;

public interface PopularSearchSvc {
  void logSearch(String keyword); // 검색어 저장
  List<PopularSearchDto> getPopularSearches(); // 인기 검색어 조회
}
