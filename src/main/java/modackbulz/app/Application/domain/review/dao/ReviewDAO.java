package modackbulz.app.Application.domain.review.dao;

import modackbulz.app.Application.entity.Review;
import java.util.List;
import java.util.Optional;

public interface ReviewDAO {
  // 캠핑장 ID로 리뷰 목록 조회
  List<Review> findByContentId(Long contentId);

  // 리뷰 단건 조회
  Optional<Review> findById(Long revId);

  // 리뷰 등록
  Review save(Review review);

  // 리뷰 수정
  int update(Review review);

  // 리뷰 삭제
  int delete(Long revId);

}
