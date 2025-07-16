package modackbulz.app.Application.domain.keyword.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.keyword.dao.KeywordDAO;
import modackbulz.app.Application.entity.Keyword;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordSVCImpl implements KeywordSVC {

  private final KeywordDAO keywordDAO;

  @Override
  public List<Keyword> findAll() {
    // 별도 로직 없이 DAO를 그대로 호출
    return keywordDAO.findAll();
  }
}