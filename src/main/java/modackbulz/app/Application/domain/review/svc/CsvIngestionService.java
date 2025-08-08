package modackbulz.app.Application.domain.review.svc;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.entity.ReviewCsvDto;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CsvIngestionService {

  private final ElasticsearchClient esClient;

  public void ingestReviewsFromCsv(String filePath) throws Exception{
    // CSV 파일 경로를 지정하고 Reader를 생성
    // Path.get()을 사용해 파일 시스템의 경로 가져옴
    Reader reader = new FileReader(Paths.get(filePath).toFile());

    // CsvToBeanBuilder를 사용해 Csv 데이터를 ReviewCsvDto 리스트로 변환
    List<ReviewCsvDto> reviewDtoList = new CsvToBeanBuilder<ReviewCsvDto>(reader)
        .withType(ReviewCsvDto.class) // 어떤 DTO 클래스에 담을지 지정)
        .withSkipLines(1) // CSV 파일의 첫 줄(헤더)를 건너뛰고 읽도록 설정
        .build()
        .parse();


    // 엘라스틱서치에 대량으로 데이터를 넣기위한 BulkRequest를 준비
    BulkRequest.Builder br = new BulkRequest.Builder();


    // DTO 리스트를 순회하면서, 각 DTO를 엘라스틱서치에 저장하라는 작업 제작
    for (ReviewCsvDto reviewCsvDto : reviewDtoList){
      br.operations(op -> op
          .index(idx -> idx
              .index("reviews") // "reviews" 인덱스에 저장
              // .id(...) // 고유 ID가 있다면 여기에 지정 가능
              .document(reviewCsvDto) // 저장할 데이터 (DTO객체)
          )
      );
    }

    // 준비된 모든 작업을 엘라스틱서치에 한 번에 전송
    BulkResponse result = esClient.bulk(br.build());

    // 결과 로깅 ( 에러 확인 )
    if (result.errors()){
      log.error("Bulk indexing에 에러가 발생했습니다.");
      for (BulkResponseItem item : result.items()){
        if (item.error() != null){
          log.error(item.error().reason());
        }
      }
    }else {
      log.info("총 {}개의 리뷰를 성공적으로 인덱싱 완료 했습니다.", reviewDtoList.size());
    }
  }
}
