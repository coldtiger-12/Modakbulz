package modackbulz.app.Application.domain.popular.dao;

import modackbulz.app.Application.domain.popular.dto.PopularSearchDto;

import java.util.List;

public interface PopularSearchDao {
  void saveSearchKeyword(String keyword); // 사용자 검색어 저장
  List<PopularSearchDto> getPopularSearches(); // 인기 검색어 조회
}
