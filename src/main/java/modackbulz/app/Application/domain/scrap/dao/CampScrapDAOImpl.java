package modackbulz.app.Application.domain.scrap.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.entity.CampScrap;
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
public class CampScrapDAOImpl implements CampScrapDAO {

  private final NamedParameterJdbcTemplate template;

  @Override
  public CampScrap add(CampScrap campScrap) {
    String sql = "INSERT INTO CAMP_SCRAP (SCRAP_ID, MEMBER_ID, CONTENT_ID, FACLT_NM, FIRST_IMAGE_URL, ADDR1) " +
        "VALUES (camp_scrap_id_seq.NEXTVAL, :memberId, :contentId, :facltNm, :firstImageUrl, :addr1)";
    SqlParameterSource param = new BeanPropertySqlParameterSource(campScrap);
    template.update(sql, param);
    return campScrap;
  }

  @Override
  public int delete(Long memberId, Long contentId) {
    String sql = "DELETE FROM CAMP_SCRAP WHERE MEMBER_ID = :memberId AND CONTENT_ID = :contentId";
    Map<String, Object> param = Map.of("memberId", memberId, "contentId", contentId);
    return template.update(sql, param);
  }

  @Override
  public List<CampScrap> findByMemberId(Long memberId) {
    String sql = "SELECT * FROM CAMP_SCRAP WHERE MEMBER_ID = :memberId ORDER BY CREATED_AT DESC";
    Map<String, Long> param = Map.of("memberId", memberId);
    return template.query(sql, param, BeanPropertyRowMapper.newInstance(CampScrap.class));
  }

  @Override
  public Optional<CampScrap> findByMemberIdAndContentId(Long memberId, Long contentId) {
    String sql = "SELECT * FROM CAMP_SCRAP WHERE MEMBER_ID = :memberId AND CONTENT_ID = :contentId";
    Map<String, Object> param = Map.of("memberId", memberId, "contentId", contentId);
    try {
      CampScrap scrap = template.queryForObject(sql, param, BeanPropertyRowMapper.newInstance(CampScrap.class));
      return Optional.of(scrap);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }
}