package modackbulz.app.Application.domain.review.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.entity.Review;
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
public class ReviewDAOImpl implements ReviewDAO {

  private final NamedParameterJdbcTemplate template;

  /**
   * 특정 캠핑장(contentId)에 해당하는 모든 리뷰를 최신순으로 조회합니다.
   */
  @Override
  public List<Review> findByContentId(Long contentId) {
    String sql = "SELECT REV_ID, CONTENT_ID, MEMBER_ID, WRITER, CONTENT, CREATED_AT, UPDATED_AT, SCORE FROM REVIEW WHERE CONTENT_ID = :contentId ORDER BY CREATED_AT DESC";
    Map<String, Object> param = Map.of("contentId", contentId);
    // SQL 실행 결과를 Review 객체 리스트로 변환하여 반환
    return template.query(sql, param, BeanPropertyRowMapper.newInstance(Review.class));
  }

  /**
   * 리뷰 ID(revId)로 특정 리뷰 1건을 조회합니다.
   */
  @Override
  public Optional<Review> findById(Long revId) {
    String sql = "SELECT REV_ID, CONTENT_ID, MEMBER_ID, WRITER, CONTENT, CREATED_AT, UPDATED_AT, SCORE FROM REVIEW WHERE REV_ID = :revId";
    Map<String, Object> param = Map.of("revId", revId);
    try {
      // SQL 실행 결과를 Review 객체로 변환
      Review review = template.queryForObject(sql, param, BeanPropertyRowMapper.newInstance(Review.class));
      // Optional.of()를 사용하여 null이 아닌 객체를 포함하는 Optional 반환
      return Optional.of(review);
    } catch (EmptyResultDataAccessException e) {
      // 결과가 없을 경우, 빈 Optional 반환
      return Optional.empty();
    }
  }

  /**
   * 새로운 리뷰를 데이터베이스에 등록합니다.
   * review_rev_id_seq 시퀀스를 사용하여 REV_ID를 자동 생성합니다.
   */
  @Override
  public Review save(Review review) {
    String sql = "INSERT INTO REVIEW (REV_ID, CONTENT_ID, MEMBER_ID, WRITER, CONTENT, SCORE) " +
        "VALUES (review_rev_id_seq.NEXTVAL, :contentId, :memberId, :writer, :content, :score)";
    // Review 객체의 필드를 SQL 파라미터로 매핑
    SqlParameterSource param = new BeanPropertySqlParameterSource(review);
    template.update(sql, param);
    return review;
  }

  /**
   * 기존 리뷰의 내용과 평점을 수정합니다.
   * SQL의 trg_set_review_updated_at 트리거에 의해 UPDATED_AT 컬럼은 자동으로 현재 시각으로 갱신됩니다.
   */
  @Override
  public int update(Review review) {
    String sql = "UPDATE REVIEW SET CONTENT = :content, SCORE = :score WHERE REV_ID = :revId";
    SqlParameterSource param = new BeanPropertySqlParameterSource(review);
    return template.update(sql, param);
  }

  /**
   * 특정 리뷰를 데이터베이스에서 삭제합니다.
   */
  @Override
  public int delete(Long revId) {
    String sql = "DELETE FROM REVIEW WHERE REV_ID = :revId";
    Map<String, Object> param = Map.of("revId", revId);
    return template.update(sql, param);
  }
}
