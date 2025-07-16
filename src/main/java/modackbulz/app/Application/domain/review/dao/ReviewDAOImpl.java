package modackbulz.app.Application.domain.review.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.entity.Review;
import modackbulz.app.Application.entity.UploadFile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDAOImpl implements ReviewDAO {

  private final NamedParameterJdbcTemplate template;

  @Override
  public List<Review> findByContentId(Long contentId) {
    String sql = "SELECT REV_ID, CONTENT_ID, MEMBER_ID, WRITER, CONTENT, CREATED_AT, UPDATED_AT, SCORE FROM REVIEW WHERE CONTENT_ID = :contentId ORDER BY CREATED_AT DESC";
    Map<String, Object> param = Map.of("contentId", contentId);
    List<Review> reviews = template.query(sql, param, BeanPropertyRowMapper.newInstance(Review.class));

    reviews.forEach(review -> {
      review.setFiles(findFilesByRevId(review.getRevId()));
      review.setKeywordIds(findKeywordIdsByRevId(review.getRevId()));
    });
    return reviews;
  }

  @Override
  public Optional<Review> findById(Long revId) {
    String sql = "SELECT REV_ID, CONTENT_ID, MEMBER_ID, WRITER, CONTENT, CREATED_AT, UPDATED_AT, SCORE FROM REVIEW WHERE REV_ID = :revId";
    Map<String, Object> param = Map.of("revId", revId);
    try {
      Review review = template.queryForObject(sql, param, BeanPropertyRowMapper.newInstance(Review.class));
      if (review != null) {
        review.setFiles(findFilesByRevId(review.getRevId()));
        review.setKeywordIds(findKeywordIdsByRevId(review.getRevId()));
      }
      return Optional.ofNullable(review);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public Long save(Review review) {
    String sql = "INSERT INTO REVIEW (REV_ID, CONTENT_ID, MEMBER_ID, WRITER, CONTENT, SCORE) " +
        "VALUES (review_rev_id_seq.NEXTVAL, :contentId, :memberId, :writer, :content, :score)";

    SqlParameterSource param = new BeanPropertySqlParameterSource(review);
    KeyHolder keyHolder = new GeneratedKeyHolder();
    template.update(sql, param, keyHolder, new String[]{"REV_ID"});
    return keyHolder.getKey().longValue();
  }

  @Override
  public int update(Review review) {
    String sql = "UPDATE REVIEW SET CONTENT = :content, SCORE = :score, UPDATED_AT = SYSTIMESTAMP WHERE REV_ID = :revId";
    SqlParameterSource param = new BeanPropertySqlParameterSource(review);
    return template.update(sql, param);
  }

  @Override
  public int delete(Long revId) {
    String sql = "DELETE FROM REVIEW WHERE REV_ID = :revId";
    return template.update(sql, Map.of("revId", revId));
  }

  @Override
  public void insertKeywords(Long revId, List<Long> keywordIds) {
    String sql = "INSERT INTO REVIEW_KEYWORD (REV_ID, KEYWORD_ID) VALUES (:revId, :keywordId)";
    for (Long keywordId : keywordIds) {
      template.update(sql, Map.of("revId", revId, "keywordId", keywordId));
    }
  }

  @Override
  public void deleteKeywordsByRevId(Long revId) {
    String sql = "DELETE FROM REVIEW_KEYWORD WHERE REV_ID = :revId";
    template.update(sql, Map.of("revId", revId));
  }

  @Override
  public List<Long> findKeywordIdsByRevId(Long revId) {
    String sql = "SELECT KEYWORD_ID FROM REVIEW_KEYWORD WHERE REV_ID = :revId";
    return template.queryForList(sql, Map.of("revId", revId), Long.class);
  }

  @Override
  public void insertFiles(Long revId, List<UploadFile> files) {
    String sql = """
        INSERT INTO FILES (
            FILE_ID, ORIGIN_NAME, SAVE_NAME, FILE_PATH,
            FILE_URL, BOARD_TYPE, BOARD_ID, UPLOAD_AT
        ) VALUES (
            FILES_SEQ.NEXTVAL, :originName, :saveName, :filePath,
            :fileUrl, 'REVIEW', :boardId, SYSTIMESTAMP
        )
    """;
    for (UploadFile file : files) {
      MapSqlParameterSource params = new MapSqlParameterSource();
      params.addValue("originName", file.getOriginName());
      params.addValue("saveName", file.getSaveName());
      params.addValue("filePath", file.getFilePath());
      params.addValue("fileUrl", file.getFileUrl());
      params.addValue("boardId", revId);
      template.update(sql, params);
    }
  }

  @Override
  public List<UploadFile> findFilesByRevId(Long revId) {
    String sql = "SELECT FILE_ID, ORIGIN_NAME, SAVE_NAME, FILE_PATH, FILE_URL FROM FILES WHERE BOARD_TYPE = 'REVIEW' AND BOARD_ID = :revId";
    return template.query(sql, Map.of("revId", revId), BeanPropertyRowMapper.newInstance(UploadFile.class));
  }

  @Override
  public void deleteFileById(Long fileId) {
    String sql = "DELETE FROM FILES WHERE FILE_ID = :fileId";
    template.update(sql, Map.of("fileId", fileId));
  }

  @Override
  public Optional<Double> calculateAverageScore(Long contentId) {
    String sql = "SELECT AVG(SCORE) FROM REVIEW WHERE CONTENT_ID = :contentId";
    Map<String, Object> param = Map.of("contentId", contentId);
    try {
      Double avgScore = template.queryForObject(sql, param, Double.class);
      return Optional.ofNullable(avgScore);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public void updateCampsiteScore(Long contentId, double score) {
    String sql = "UPDATE CAMPSITES SET SCORE = :score WHERE CONTENT_ID = :contentId";
    Map<String, Object> param = Map.of(
        "score", Math.round(score * 10.0) / 10.0,
        "contentId", contentId
    );
    template.update(sql, param);
  }
}