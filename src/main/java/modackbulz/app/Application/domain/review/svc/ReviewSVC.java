package modackbulz.app.Application.domain.review.svc;

import modackbulz.app.Application.entity.Review;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewSVC {

  List<Review> findByContentId(Long contentId);

  Optional<Review> findById(Long revId);

  Long save(Review review);

  // [수정] 수정 시 삭제할 파일 ID 목록을 함께 받도록 시그니처 변경
  int update(Review review, List<Long> deletedFileIds);

  int delete(Long revId);

  Double calculateAverageScore(Long contentId);

  Map<Integer, Long> calculateScoreDistribution(Long contentId);
}