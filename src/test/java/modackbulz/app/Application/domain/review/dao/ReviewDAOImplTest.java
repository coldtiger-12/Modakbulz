package modackbulz.app.Application.domain.review.dao;

import modackbulz.app.Application.entity.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReviewDAOImpl의 각 메소드를 테스트하는 클래스입니다.
 * @SpringBootTest: 스프링 부트의 전체 애플리케이션 컨텍스트를 로드하여 통합 테스트를 수행합니다.
 * @Transactional: 각 테스트 메소드가 끝날 때마다 데이터베이스 변경 사항을 롤백하여
 * 테스트가 서로에게 영향을 주지 않도록 격리합니다.
 */
@SpringBootTest
@Transactional
class ReviewDAOImplTest {

  @Autowired
  private ReviewDAO reviewDAO;

  @Autowired
  private JdbcTemplate jdbcTemplate; // 테스트 데이터 준비를 위해 사용

  // 테스트에 필요한 고정된 ID 값들
  private static final Long TEST_MEMBER_ID = 999L;
  private static final Long TEST_CONTENT_ID = 9999L;

  /**
   * 각 테스트 실행 전에, 외래 키 제약 조건을 만족시키기 위한
   * 테스트용 회원 및 캠핑장 데이터를 미리 삽입합니다.
   */
  @BeforeEach
  void setUp() {
    // 테스트용 회원 데이터 삽입
    jdbcTemplate.update("INSERT INTO MEMBER(MEMBER_ID, ID, PWD, EMAIL, TEL, NICKNAME, GUBUN) VALUES (?, ?, ?, ?, ?, ?, ?)",
        TEST_MEMBER_ID, "testuser", "Test1234!", "test@test.com", "01012345678", "테스터", "U");

    // 테스트용 캠핑장 정보 데이터 삽입
    jdbcTemplate.update("INSERT INTO CAMPING_INFO(contentId, facltNm) VALUES (?, ?)",
        TEST_CONTENT_ID, "테스트 캠핑장");

    // 테스트용 캠핑장 사이트 데이터 삽입
    jdbcTemplate.update("INSERT INTO CAMPSITES(CONTENT_ID) VALUES (?)",
        TEST_CONTENT_ID);
  }

  @Test
  @DisplayName("리뷰 저장 및 ID로 조회 테스트")
  void saveAndFindById() {
    // given: 저장할 리뷰 객체 생성
    Review newReview = createReview("정말 좋은 캠핑장이었어요!", 5);

    // when: 리뷰를 저장
    reviewDAO.save(newReview);

    // then: 저장된 리뷰를 다시 조회하여 검증
    // 참고: 현재 save 메소드는 생성된 ID를 반환하지 않으므로, contentId로 전체 조회 후 첫 번째 항목을 가져와 검증
    List<Review> reviews = reviewDAO.findByContentId(TEST_CONTENT_ID);
    assertThat(reviews).isNotEmpty();
    Review savedReview = reviews.get(0); // 최신순 정렬이므로 첫 번째가 방금 저장한 리뷰

    Optional<Review> foundReviewOpt = reviewDAO.findById(savedReview.getRevId());

    assertThat(foundReviewOpt).isPresent();
    Review foundReview = foundReviewOpt.get();
    assertThat(foundReview.getContent()).isEqualTo("정말 좋은 캠핑장이었어요!");
    assertThat(foundReview.getWriter()).isEqualTo("테스터");
    assertThat(foundReview.getScore()).isEqualTo(5);
  }

  @Test
  @DisplayName("캠핑장 ID로 리뷰 목록 조회 테스트")
  void findByContentId() {
    // given: 2개의 리뷰를 같은 캠핑장 ID로 저장
    reviewDAO.save(createReview("첫 번째 리뷰", 4));
    reviewDAO.save(createReview("두 번째 리뷰", 5));

    // when: 해당 캠핑장 ID로 리뷰 목록을 조회
    List<Review> reviews = reviewDAO.findByContentId(TEST_CONTENT_ID);

    // then: 조회된 리뷰 목록의 크기가 2인지 확인
    assertThat(reviews).hasSize(2);
    // 최신순으로 정렬되었으므로, 첫 번째 리뷰의 내용이 "두 번째 리뷰"인지 확인
    assertThat(reviews.get(0).getContent()).isEqualTo("두 번째 리뷰");
  }

  @Test
  @DisplayName("리뷰 수정 테스트")
  void update() {
    // given: 초기 리뷰 저장
    Review originalReview = createReview("수정 전 내용입니다.", 3);
    reviewDAO.save(originalReview);
    Long reviewId = reviewDAO.findByContentId(TEST_CONTENT_ID).get(0).getRevId();

    // when: 저장된 리뷰의 내용과 평점을 수정
    Review updatedReviewInfo = new Review();
    updatedReviewInfo.setRevId(reviewId);
    updatedReviewInfo.setContent("리뷰 내용을 수정했습니다.");
    updatedReviewInfo.setScore(5);
    int updateCount = reviewDAO.update(updatedReviewInfo);

    // then: 수정이 성공적으로 이루어졌는지, 데이터가 변경되었는지 확인
    assertThat(updateCount).isEqualTo(1);
    Optional<Review> foundAfterUpdate = reviewDAO.findById(reviewId);
    assertThat(foundAfterUpdate).isPresent();
    assertThat(foundAfterUpdate.get().getContent()).isEqualTo("리뷰 내용을 수정했습니다.");
    assertThat(foundAfterUpdate.get().getScore()).isEqualTo(5);
  }

  @Test
  @DisplayName("리뷰 삭제 테스트")
  void delete() {
    // given: 리뷰를 저장하고 ID를 확인
    Review reviewToDelete = createReview("곧 삭제될 리뷰입니다.", 1);
    reviewDAO.save(reviewToDelete);
    Long reviewId = reviewDAO.findByContentId(TEST_CONTENT_ID).get(0).getRevId();

    // when: 해당 리뷰를 삭제
    int deleteCount = reviewDAO.delete(reviewId);

    // then: 삭제가 성공했는지, 실제로 데이터가 사라졌는지 확인
    assertThat(deleteCount).isEqualTo(1);
    Optional<Review> foundAfterDelete = reviewDAO.findById(reviewId);
    assertThat(foundAfterDelete).isEmpty();
  }

  /**
   * 테스트용 Review 객체를 생성하는 헬퍼 메소드
   */
  private Review createReview(String content, int score) {
    Review review = new Review();
    review.setContentId(TEST_CONTENT_ID);
    review.setMemberId(TEST_MEMBER_ID);
    review.setWriter("테스터");
    review.setContent(content);
    review.setScore(score);
    return review;
  }
}