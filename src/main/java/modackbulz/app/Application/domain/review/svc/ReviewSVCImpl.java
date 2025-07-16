package modackbulz.app.Application.domain.review.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.common.FileStore;
import modackbulz.app.Application.domain.review.dao.ReviewDAO;
import modackbulz.app.Application.entity.Review;
import modackbulz.app.Application.entity.UploadFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewSVCImpl implements ReviewSVC {

  private final ReviewDAO reviewDAO;
  private final FileStore fileStore;

  @Override
  public List<Review> findByContentId(Long contentId) {
    return reviewDAO.findByContentId(contentId);
  }

  @Override
  public Optional<Review> findById(Long revId) {
    return reviewDAO.findById(revId);
  }

  @Override
  @Transactional
  public Long save(Review review) {
    Long revId = reviewDAO.save(review);

    List<Long> keywordIds = review.getKeywordIds();
    if (keywordIds != null && !keywordIds.isEmpty()) {
      reviewDAO.insertKeywords(revId, keywordIds);
    }

    List<UploadFile> files = review.getFiles();
    if (files != null && !files.isEmpty()) {
      reviewDAO.insertFiles(revId, files);
    }

    updateAverageScore(review.getContentId());

    return revId;
  }

  @Override
  @Transactional
  public int update(Review review, List<Long> deletedFileIds) {
    // 1. 리뷰 기본 정보(내용, 평점) 수정
    int affectedRows = reviewDAO.update(review);
    if(affectedRows == 0) return 0;

    Long revId = review.getRevId();

    // 2. 키워드 수정 (기존 것 모두 삭제 후 새로 삽입)
    reviewDAO.deleteKeywordsByRevId(revId);
    List<Long> keywordIds = review.getKeywordIds();
    if (keywordIds != null && !keywordIds.isEmpty()) {
      reviewDAO.insertKeywords(revId, keywordIds);
    }

    // 3. 파일 DB 정보 삭제
    if (deletedFileIds != null && !deletedFileIds.isEmpty()) {
      for(Long fileId : deletedFileIds) {
        reviewDAO.deleteFileById(fileId);
      }
    }

    // 4. 새로 추가된 파일들 DB에 저장
    List<UploadFile> newFiles = review.getFiles();
    if (newFiles != null && !newFiles.isEmpty()) {
      reviewDAO.insertFiles(revId, newFiles);
    }

    // 5. 평균 점수 업데이트
    updateAverageScore(review.getContentId());

    return affectedRows;
  }

  @Override
  @Transactional
  public int delete(Long revId) {
    Optional<Review> optionalReview = reviewDAO.findById(revId);
    if (optionalReview.isEmpty()) {
      return 0;
    }
    Review review = optionalReview.get();
    Long contentId = review.getContentId();

    // 1. 서버에 저장된 물리적 파일 삭제
    fileStore.deleteFiles(review.getFiles());

    // 2. DB에서 리뷰 삭제 (연관된 키워드, 파일 정보도 CASCADE로 함께 삭제됨)
    int affectedRows = reviewDAO.delete(revId);

    // 3. 평균 점수 업데이트
    if (affectedRows > 0) {
      updateAverageScore(contentId);
    }

    return affectedRows;
  }

  private void updateAverageScore(Long contentId) {
    double averageScore = reviewDAO.calculateAverageScore(contentId).orElse(0.0);
    reviewDAO.updateCampsiteScore(contentId, averageScore);
  }

  @Override
  public Double calculateAverageScore(Long contentId) {
    return reviewDAO.calculateAverageScore(contentId).orElse(0.0);
  }

  @Override
  public Map<Integer, Long> calculateScoreDistribution(Long contentId) {
    List<Review> reviews = findByContentId(contentId);
    return reviews.stream()
        .collect(Collectors.groupingBy(
            Review::getScore,
            Collectors.counting()
        ));
  }
}