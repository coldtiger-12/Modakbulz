package modackbulz.app.Application.domain.camping.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CampingDAOImpl implements CampingDAO {
  private final NamedParameterJdbcTemplate template;

  @Override
  public void saveOrUpdate(GoCampingDto.Item item) {
    String sql = """
            MERGE INTO CAMPING_INFO T
            USING (SELECT :contentId AS contentId FROM DUAL) S
            ON (T.contentId = S.contentId)
            WHEN MATCHED THEN
                UPDATE SET facltNm = :facltNm, addr1 = :addr1, firstImageUrl = :firstImageUrl,
                           lineIntro = :lineIntro, intro = :intro, tel = :tel, homepage = :homepage,
                           sbrsCl = :sbrsCl, themaEnvrnCl = :themaEnvrnCl,
                           featureNm = :featureNm, induty = :induty,
                           lctCl = :lctCl, operPdCl = :operPdCl, gnrlSiteCo = :gnrlSiteCo,
                           autoSiteCo = :autoSiteCo, glampSiteCo = :glampSiteCo, caravSiteCo = :caravSiteCo
            WHEN NOT MATCHED THEN
                INSERT (contentId, facltNm, addr1, firstImageUrl, lineIntro, intro, tel, homepage, sbrsCl, themaEnvrnCl, featureNm, induty, lctCl, operPdCl, gnrlSiteCo, autoSiteCo, glampSiteCo, caravSiteCo)
                VALUES (:contentId, :facltNm, :addr1, :firstImageUrl, :lineIntro, :intro, :tel, :homepage, :sbrsCl, :themaEnvrnCl, :featureNm, :induty, :lctCl, :operPdCl, :gnrlSiteCo, :autoSiteCo, :glampSiteCo, :caravSiteCo)
        """;
    template.update(sql, new BeanPropertySqlParameterSource(item));
  }

  @Override
  public Optional<GoCampingDto.Item> findByContentId(Long contentId) {
    String sql = "SELECT * FROM CAMPING_INFO WHERE contentId = :contentId";
    try {
      GoCampingDto.Item item = template.queryForObject(sql, Map.of("contentId", contentId), new BeanPropertyRowMapper<>(GoCampingDto.Item.class));
      return Optional.ofNullable(item);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public Page<GoCampingDto.Item> findAll(Pageable pageable) {
    String countSql = "SELECT count(*) FROM CAMPING_INFO";
    int total = template.getJdbcTemplate().queryForObject(countSql, Integer.class);

    String sql = """
            SELECT * FROM (
                SELECT ROWNUM AS rnum, c.* FROM (
                    SELECT * FROM CAMPING_INFO 
                    ORDER BY CASE WHEN firstImageUrl IS NOT NULL THEN 0 ELSE 1 END, contentId DESC
                ) c
            ) WHERE rnum BETWEEN :startRow AND :endRow
        """;

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("startRow", pageable.getOffset() + 1);
    params.addValue("endRow", pageable.getOffset() + pageable.getPageSize());

    List<GoCampingDto.Item> content = template.query(sql, params, new BeanPropertyRowMapper<>(GoCampingDto.Item.class));

    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public Page<GoCampingDto.Item> search(String keyword, Pageable pageable) {
    String countSql = "SELECT count(*) FROM CAMPING_INFO WHERE facltNm LIKE :keyword OR addr1 LIKE :keyword OR sbrsCl LIKE :keyword OR themaEnvrnCl LIKE :keyword";
    MapSqlParameterSource countParams = new MapSqlParameterSource("keyword", "%" + keyword + "%");
    int total = template.queryForObject(countSql, countParams, Integer.class);

    String sql = """
            SELECT * FROM (
                SELECT ROWNUM AS rnum, c.* FROM (
                    SELECT * FROM CAMPING_INFO
                    WHERE facltNm LIKE :keyword OR addr1 LIKE :keyword OR sbrsCl LIKE :keyword OR themaEnvrnCl LIKE :keyword
                    ORDER BY CASE WHEN firstImageUrl IS NOT NULL THEN 0 ELSE 1 END, contentId DESC
                ) c
            ) WHERE rnum BETWEEN :startRow AND :endRow
        """;

    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("keyword", "%" + keyword + "%");
    params.addValue("startRow", pageable.getOffset() + 1);
    params.addValue("endRow", pageable.getOffset() + pageable.getPageSize());

    List<GoCampingDto.Item> content = template.query(sql, params, new BeanPropertyRowMapper<>(GoCampingDto.Item.class));
    return new PageImpl<>(content, pageable, total);
  }
}