package modackbulz.app.Application.domain.keyword.dao;

import modackbulz.app.Application.entity.Keyword;
import java.util.List;

public interface KeywordDAO {
  List<Keyword> findAll();
}