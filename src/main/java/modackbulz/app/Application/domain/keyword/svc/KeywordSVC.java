package modackbulz.app.Application.domain.keyword.svc;

import modackbulz.app.Application.entity.Keyword;
import java.util.List;

public interface KeywordSVC {
  List<Keyword> findAll();
}