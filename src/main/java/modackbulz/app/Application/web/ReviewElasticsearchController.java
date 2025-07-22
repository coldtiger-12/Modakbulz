//package modackbulz.app.Application.web;
//
//import modackbulz.app.Application.domain.review.svc.ReviewElasticsearchService;
//import modackbulz.app.Application.entity.Review;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/elasticsearch/reviews")
//public class eviewElasticsearchController {
//
//  @Autowired
//  private ReviewElasticsearchService reviewElasticsearchService;
//
//  // 모든 리뷰 조회
//  @GetMapping
//  public ResponseEntity<List<Review>> getAllReviews() {
//    List<Review> reviews = reviewElasticsearchService.findAllReviews();
//    return ResponseEntity.ok(reviews);
//  }
//
//  // ID로 리뷰 조회
//  @GetMapping("/{id}")
//  public ResponseEntity<Review> getReviewById(@PathVariable String id) {
//    Review review = reviewElasticsearchService.findById(id);
//    if (review != null) {
//      return ResponseEntity.ok(review);
//    } else {
//      return ResponseEntity.notFound().build();
//    }
//  }
//
//  // 캠핑장 ID로 리뷰 조회
//  @GetMapping("/camping/{contentId}")
//  public ResponseEntity<List<Review>> getReviewsByContentId(@PathVariable Long contentId) {
//    List<Review> reviews = reviewElasticsearchService.findByContentId(contentId);
//    return ResponseEntity.ok(reviews);
//  }
//
//  // 텍스트 검색
//  @GetMapping("/search")
//  public ResponseEntity<List<Review>> searchReviews(@RequestParam String keyword) {
//    List<Review> reviews = reviewElasticsearchService.searchByContent(keyword);
//    return ResponseEntity.ok(reviews);
//  }
//
//  // 평점 범위로 검색
//  @GetMapping("/score-range")
//  public ResponseEntity<List<Review>> getReviewsByScoreRange(
//      @RequestParam int minScore,
//      @RequestParam int maxScore) {
//    List<Review> reviews = reviewElasticsearchService.findByScoreRange(minScore, maxScore);
//    return ResponseEntity.ok(reviews);
//  }
//
//  // 리뷰 저장
//  @PostMapping
//  public ResponseEntity<Review> saveReview(@RequestBody Review review) {
//    Review savedReview = reviewElasticsearchService.saveReview(review);
//    return ResponseEntity.ok(savedReview);
//  }
//
//  // 리뷰 삭제
//  @DeleteMapping("/{id}")
//  public ResponseEntity<Void> deleteReview(@PathVariable String id) {
//    reviewElasticsearchService.deleteReview(id);
//    return ResponseEntity.ok().build();
//  }
//
//  // 캠핑장 ID로 리뷰 삭제
//  @DeleteMapping("/camping/{contentId}")
//  public ResponseEntity<Void> deleteReviewsByContentId(@PathVariable Long contentId) {
//    reviewElasticsearchService.deleteByContentId(contentId);
//    return ResponseEntity.ok().build();
//  }
//}