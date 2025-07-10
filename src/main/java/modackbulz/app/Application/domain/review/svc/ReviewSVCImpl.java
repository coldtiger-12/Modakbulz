package modackbulz.app.Application.domain.review.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.review.dao.ReviewDAO;
import modackbulz.app.Application.entity.Review;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewSVCImpl implements ReviewSVC {

  private final ReviewDAO reviewDAO;

  /**
   * DAO를 호출하여 특정 캠핑장의 리뷰 목록을 그대로 반환합니다.
   */
  @Override
  public List<Review> findByContentId(Long contentId) {
    return reviewDAO.findByContentId(contentId);
  }

  /**
   * DAO를 호출하여 특정 리뷰를 그대로 반환합니다.
   */
  @Override
  public Optional<Review> findById(Long revId) {
    return reviewDAO.findById(revId);
  }

  /**
   * DAO를 호출하여 리뷰를 저장합니다.
   * 추가적인 비즈니스 로직(예: 포인트 적립)이 필요하다면 여기에 구현할 수 있습니다.
   */
  @Override
  public Review save(Review review) {
    return reviewDAO.save(review);
  }

  /**
   * DAO를 호출하여 리뷰를 수정합니다.
   * 권한 검사 등의 로직은 컨트롤러 단에서 처리하거나,
   * 파라미터로 로그인 정보를 받아와 여기서 처리할 수 있습니다.
   */
  @Override
  public int update(Review review) {
    return reviewDAO.update(review);
  }

  /**
   * DAO를 호출하여 리뷰를 삭제합니다.
   * 권한 검사 로직은 컨트롤러에서 처리하는 것을 권장합니다.
   */
  @Override
  public int delete(Long revId) {
    return reviewDAO.delete(revId);
  }
}
