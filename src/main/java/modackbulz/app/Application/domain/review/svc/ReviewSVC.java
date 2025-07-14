package modackbulz.app.Application.domain.review.svc;

import modackbulz.app.Application.entity.Review;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ReviewSVC {

  /**
   * 특정 캠핑장에 달린 모든 리뷰를 가져옵니다.
   * @param contentId 캠핑장 ID
   * @return 리뷰 엔티티 리스트
   */
  List<Review> findByContentId(Long contentId);

  /**
   * ID로 특정 리뷰 하나를 조회합니다.
   * @param revId 리뷰 ID
   * @return Optional<Review>
   */
  Optional<Review> findById(Long revId);

  /**
   * 새로운 리뷰를 등록합니다.
   * @param review 등록할 리뷰 정보
   * @return 등록된 리뷰 정보
   */
  Review save(Review review);

  /**
   * 기존 리뷰를 수정합니다.
   * @param review 수정할 리뷰 정보
   * @return 수정 성공 시 1, 실패 시 0
   */
  int update(Review review);

  /**
   * 리뷰를 삭제합니다.
   * @param revId 삭제할 리뷰 ID
   * @return 삭제 성공 시 1, 실패 시 0
   */
  int delete(Long revId);

  /**
   *  별점 평균값 구하기
   */
  Double calculateAverageScore(Long contentId);

  /**
   * 별점 분포를 계산합니다. (예: 5점: 3개, 4점: 2개 ...)
   * @param contentId 캠핑장 ID
   * @return 점수별 개수 분포 Map (key: 점수, value: 개수)
   */
  Map<Integer, Long> calculateScoreDistribution(Long contentId);
}

