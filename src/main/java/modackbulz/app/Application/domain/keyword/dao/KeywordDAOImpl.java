package modackbulz.app.Application.domain.keyword.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.entity.Keyword;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KeywordDAOImpl implements KeywordDAO {

  private final NamedParameterJdbcTemplate template;

  @Override
  public List<Keyword> findAll() {
    // KEYWORD 테이블의 모든 데이터를 조회하는 SQL
    String sql = "SELECT KEYWORD_ID, WORDS FROM KEYWORD ORDER BY KEYWORD_ID ASC";
    // 결과를 Keyword 객체 리스트로 변환하여 반환
    return template.query(sql, BeanPropertyRowMapper.newInstance(Keyword.class));
  }
}