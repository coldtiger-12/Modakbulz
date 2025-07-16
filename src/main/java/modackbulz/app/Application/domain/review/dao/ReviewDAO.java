package modackbulz.app.Application.domain.review.dao;

import modackbulz.app.Application.entity.Review;
import modackbulz.app.Application.entity.UploadFile;

import java.util.List;
import java.util.Optional;

public interface ReviewDAO {

  List<Review> findByContentId(Long contentId);

  Optional<Review> findById(Long revId);

  Long save(Review review);

  int update(Review review);

  int delete(Long revId);

  // --- 키워드 관련 ---
  void insertKeywords(Long revId, List<Long> keywordIds);

  void deleteKeywordsByRevId(Long revId);

  List<Long> findKeywordIdsByRevId(Long revId);

  // --- 파일 관련 ---
  void insertFiles(Long revId, List<UploadFile> files);

  List<UploadFile> findFilesByRevId(Long revId);

  void deleteFileById(Long fileId);

  // --- 점수 계산 관련 ---
  Optional<Double> calculateAverageScore(Long contentId);

  void updateCampsiteScore(Long contentId, double score);
}