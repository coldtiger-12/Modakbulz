package modackbulz.app.Application.domain.popular.svc;

import modackbulz.app.Application.domain.popular.dao.PopularSearchDao;
import modackbulz.app.Application.domain.popular.dto.PopularSearchDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PopularSearchSvcImpl implements PopularSearchSvc {

  private final PopularSearchDao popularSearchDao;

  public PopularSearchSvcImpl(PopularSearchDao popularSearchDao) {
    this.popularSearchDao = popularSearchDao;
  }

  @Override
  public void logSearch(String keyword) {
    popularSearchDao.saveSearchKeyword(keyword);
  }

  @Override
  public List<PopularSearchDto> getPopularSearches() {
    return popularSearchDao.getPopularSearches();
  }
}
