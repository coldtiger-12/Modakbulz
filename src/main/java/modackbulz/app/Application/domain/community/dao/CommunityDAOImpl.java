package modackbulz.app.Application.domain.community.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.entity.Community;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommunityDAOImpl implements CommunityDAO {

  private final NamedParameterJdbcTemplate template;

  @Override
  public List<Community> findAll() {
    String sql = "SELECT * FROM COMMUNITY ORDER BY CO_ID DESC";
    return template.query(sql, BeanPropertyRowMapper.newInstance(Community.class));
  }

  @Override
  public Optional<Community> findById(Long id) {
    String sql = "SELECT * FROM COMMUNITY WHERE CO_ID = :id";
    Map<String, Object> params = Map.of("id", id);
    try {
      Community post = template.queryForObject(sql, params, BeanPropertyRowMapper.newInstance(Community.class));
      return Optional.of(post);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public Community save(Community community) {
    String sql = "INSERT INTO COMMUNITY (CO_ID, FILE_NO, MEMBER_ID, TITLE, WRITER, CONTENT, CREATED_AT, VIEW_C) " +
        "VALUES (community_co_id_seq.NEXTVAL, :fileNo, :memberId, :title, :writer, :content, SYSTIMESTAMP, '0')";
    SqlParameterSource param = new BeanPropertySqlParameterSource(community);
    template.update(sql, param);
    return community;
  }

  @Override
  public Community update(Community community) {
    String sql = "UPDATE COMMUNITY SET TITLE = :title, CONTENT = :content, UPDATED_AT = SYSTIMESTAMP WHERE CO_ID = :coId";
    SqlParameterSource param = new BeanPropertySqlParameterSource(community);
    template.update(sql, param);
    return community;
  }

  @Override
  public void delete(Long id) {
    String sql = "DELETE FROM COMMUNITY WHERE CO_ID = :id";
    Map<String, Object> param = Map.of("id", id);
    template.update(sql, param);
  }

  @Override
  public List<Community> findByMemberId(String memberId) {
    String sql = "SELECT * FROM COMMUNITY WHERE MEMBER_ID = :memberId";
    return template.query(sql, Map.of("memberId", memberId), BeanPropertyRowMapper.newInstance(Community.class));
  }

  @Override
  public void increaseViewCount(Long id) {
    String sql = "UPDATE COMMUNITY SET VIEW_C = TO_CHAR(TO_NUMBER(NVL(VIEW_C, '0')) + 1) WHERE CO_ID = :id";
    template.update(sql, Map.of("id", id));
  }
}