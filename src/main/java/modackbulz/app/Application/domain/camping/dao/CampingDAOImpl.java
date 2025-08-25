package modackbulz.app.Application.domain.camping.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.entity.CampNm;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CampingDAOImpl implements CampingDAO {
  private final NamedParameterJdbcTemplate template;

  @Override
  @Transactional
  public void saveOrUpdate(GoCampingDto.Item item) {
    // 1. 기존 CAMPING_INFO 테이블 MERGE 구문 (변경 없음)
    String sql = """
            MERGE INTO CAMPING_INFO T
            USING (SELECT :contentId AS contentId FROM DUAL) S
            ON (T.contentId = S.contentId)
            WHEN MATCHED THEN
                UPDATE SET facltNm = :facltNm, addr1 = :addr1, firstImageUrl = :firstImageUrl,
                           lineIntro = :lineIntro, intro = :intro, tel = :tel, homepage = :homepage,
                           sbrsCl = :sbrsCl, themaEnvrnCl = :themaEnvrnCl,
                           featureNm = :featureNm, induty = :induty, doNm = :doNm,
                           lctCl = :lctCl, operPdCl = :operPdCl, gnrlSiteCo = :gnrlSiteCo,
                           autoSiteCo = :autoSiteCo, glampSiteCo = :glampSiteCo, caravSiteCo = :caravSiteCo
            WHEN NOT MATCHED THEN
                INSERT (contentId, facltNm, addr1, firstImageUrl, lineIntro, intro, tel, homepage, sbrsCl, themaEnvrnCl, featureNm, induty, lctCl, doNm, operPdCl, gnrlSiteCo, autoSiteCo, glampSiteCo, caravSiteCo)
                VALUES (:contentId, :facltNm, :addr1, :firstImageUrl, :lineIntro, :intro, :tel, :homepage, :sbrsCl, :themaEnvrnCl, :featureNm, :induty, :lctCl, :doNm, :operPdCl, :gnrlSiteCo, :autoSiteCo, :glampSiteCo, :caravSiteCo)
        """;
    template.update(sql, new BeanPropertySqlParameterSource(item));

    // 2. ✨ [추가] CAMPSITES 테이블에 자동으로 데이터를 추가하는 MERGE 구문
    String campsitesSql = """
            MERGE INTO CAMPSITES T
            USING (SELECT :contentId AS contentId FROM DUAL) S
            ON (T.CONTENT_ID = S.contentId)
            WHEN NOT MATCHED THEN
                INSERT (CONTENT_ID, SC_C, VIEW_C, SCORE)
                VALUES (:contentId, 0, 0, 0)
        """;
    template.update(campsitesSql, new BeanPropertySqlParameterSource(item));
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

  // 전체 목록 조회 ( DB -> index로 캠핑장 데이터 정보 전달 하기 위함)
  @Override
  public List<CampNm> findAllForSync(){
    // CAMPING_INFO 테이블의 모든 데이터를 조회하는 간단한 SQL
    String sql = "SELECT * FROM CAMPING_INFO";

    // BeanPropertyRowMapper를 사용해 결과를 CampNm 리스트로 자동 변환
    return template.query(sql, new BeanPropertyRowMapper<>(CampNm.class));
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

  //addr1 LIKE :keyword

  @Override
  public Page<GoCampingDto.Item> search(String keyword, Pageable pageable) {
    // 1. 검색어를 공백 기준으로 단어별로 나눕니다.
    //    trim()으로 양 끝 공백 제거, split("\\s+")으로 여러 공백도 하나로 처리
    String[] keywords = keyword.trim().split("\\s+");
    MapSqlParameterSource params = new MapSqlParameterSource();

    // 2. 동적으로 WHERE 절을 생성합니다.
    StringBuilder whereClause = new StringBuilder();
    for (int i = 0; i < keywords.length; i++) {
      String currentKeyword = keywords[i];
      if (currentKeyword == null || currentKeyword.isBlank()) {
        continue; // 비어있는 키워드는 무시
      }

      // 각 키워드를 위한 파라미터 이름을 만듭니다 (예: keyword0, keyword1)
      String paramName = "keyword" + i;

      whereClause.append(" AND (LOWER(doNm) LIKE LOWER(:" + paramName + ") ");
      whereClause.append(" OR LOWER(addr1) LIKE LOWER(:" + paramName + ") ");
      whereClause.append(" OR LOWER(sbrsCl) LIKE LOWER(:" + paramName + ") ");
      whereClause.append(" OR LOWER(themaEnvrnCl) LIKE LOWER(:" + paramName + ") ");
      whereClause.append(" OR LOWER(induty) LIKE LOWER(:" + paramName + ") ");
      whereClause.append(" OR LOWER(lctCl) LIKE LOWER(:" + paramName + ")) ");

      // 파라미터 값을 추가합니다. (예: keyword0 -> %가을단풍명소%)
      params.addValue(paramName, "%" + currentKeyword + "%");
    }

    // 3. 전체 개수를 세는 쿼리와 데이터를 가져오는 쿼리를 동적으로 완성합니다.
    String countSql = "SELECT count(*) FROM CAMPING_INFO WHERE 1=1 " + whereClause.toString();

    String sql = """
            SELECT * FROM (
                SELECT ROWNUM AS rnum, c.* FROM (
                    SELECT * FROM CAMPING_INFO
                    WHERE 1=1 %s
                    ORDER BY CASE WHEN firstImageUrl IS NOT NULL THEN 0 ELSE 1 END, contentId DESC
                ) c
            ) WHERE rnum BETWEEN :startRow AND :endRow
        """.formatted(whereClause.toString());

    // 4. 쿼리를 실행합니다.
    int total = template.queryForObject(countSql, params, Integer.class);

    params.addValue("startRow", pageable.getOffset() + 1);
    params.addValue("endRow", pageable.getOffset() + pageable.getPageSize());

    List<GoCampingDto.Item> content = template.query(sql, params, new BeanPropertyRowMapper<>(GoCampingDto.Item.class));

    return new PageImpl<>(content, pageable, total);
  }

  @Override
  public Page<GoCampingDto.Item> findAllOrderByScrapCountDesc(Pageable pageable) {
    String countSql = """
        SELECT count(*) FROM CAMPING_INFO
    """;
    int total = template.getJdbcTemplate().queryForObject(countSql, Integer.class);
    String sql = """
        SELECT * FROM (
            SELECT ROWNUM AS rnum, c.* FROM (
                SELECT ci.*
                FROM CAMPING_INFO ci
                LEFT JOIN CAMPSITES cs ON ci.contentId = cs.CONTENT_ID -- INNER JOIN -> LEFT JOIN
                ORDER BY NVL(cs.SC_C, 0) DESC, ci.contentId DESC -- 스크랩 수가 없으면(NULL) 0으로 처리
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
  public Page<GoCampingDto.Item> findAllByRegionOrderByScrapCountDesc(String region, Pageable pageable) {
    // 1. 파라미터 준비
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("region", "%" + region + "%"); // LIKE 검색을 위해 % 추가

    // 2. 해당 지역의 캠핑장 전체 개수를 세는 쿼리
    String countSql = """
        SELECT count(*)
        FROM CAMPING_INFO
        WHERE LOWER(addr1) LIKE LOWER(:region)
    """;
    int total = template.queryForObject(countSql, params, Integer.class);

    if (total == 0) {
      return Page.empty(pageable); // 결과가 없으면 비어있는 페이지 반환
    }

    // 3. 실제 데이터를 가져오는 쿼리 (페이징 적용)
    String sql = """
        SELECT * FROM (
            SELECT ROWNUM AS rnum, c.* FROM (
                SELECT ci.*
                FROM CAMPING_INFO ci
                LEFT JOIN CAMPSITES cs ON ci.contentId = cs.CONTENT_ID
                WHERE LOWER(ci.addr1) LIKE LOWER(:region) -- 지역 필터링 조건 추가
                ORDER BY NVL(cs.SC_C, 0) DESC, ci.contentId DESC
            ) c
        ) WHERE rnum BETWEEN :startRow AND :endRow
    """;

    // 4. 페이징 파라미터 추가 및 쿼리 실행
    params.addValue("startRow", pageable.getOffset() + 1);
    params.addValue("endRow", pageable.getOffset() + pageable.getPageSize());

    List<GoCampingDto.Item> content = template.query(sql, params, new BeanPropertyRowMapper<>(GoCampingDto.Item.class));

    return new PageImpl<>(content, pageable, total);
  }
}