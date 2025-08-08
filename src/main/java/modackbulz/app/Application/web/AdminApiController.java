package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.review.svc.CsvIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminApiController {

  private final CsvIngestionService csvIngestionService;

  @PostMapping("/ingest-reviews")
  public ResponseEntity<String> ingestReviews(){
    try{
      String csvFilePath = "C:/KDT/reviews/all_reviews_person_all.csv";

      csvIngestionService.ingestReviewsFromCsv(csvFilePath);
      return ResponseEntity.ok("CSV 파일 인덱싱 작업 시작");
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body("작업 실패" + e.getMessage());

    }
  }
}
