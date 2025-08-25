package modackbulz.app.Application.global.scheduler;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
import modackbulz.app.Application.entity.CampNm;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncService {

  private final CampingDAO campingDAO;
  private final ElasticsearchClient esClient;
  private final String INDEX_NAME = "camp_final";

  /**
   * 매일 오후 12시에 실행 , DB의 모든 캠핑장 데이터를
   * Elasticesarch의 camp_final 인덱스로 동기화(덮어쓰기) 함
   */

  @Scheduled(cron = "0 02 10 * * *")
  public void syncCampsToElasticsearch(){
    log.info("======= DB & CSV -> Elasticsearch 동기화 스케쥴러 시작 ========");
    try{
      // 네이버 키워드 CSV를 먼저 읽어서 Map에 저장함 (contentId -> keyword)
      Map<String, String> naverKeywordsMap = loadNaverKeywordsFromCsv("data/camping_keywords.csv");
      log.info("CSV에서 총 {}개의 네이버 키워드를 로드했습니다.", naverKeywordsMap);

      List<CampNm> allCampFromDB = campingDAO.findAllForSync();
      if (allCampFromDB == null || allCampFromDB.isEmpty()){
        log.warn("DB에 동기화할 캠핑장 데이터가 없습니다.");
        return;
      }

      BulkRequest.Builder builder = new BulkRequest.Builder();
      int successfulJoins = 0;

      for (CampNm camp : allCampFromDB){

        String naverKeyword = naverKeywordsMap.get(camp.getFacltNm());

        if (naverKeyword != null){
          log.info("[{}] 캠핑장에 키워드 '{}' 연결 성공", camp.getFacltNm(), naverKeyword);
          camp.setColumn2(naverKeyword);
          successfulJoins++;

        }

        Map<String, Object> doc = creatEsDocument(camp);

        if (doc.containsKey("column2")){
          log.info("최종 문서에 column2 필드가 포함되었습니다.", doc.get("column2"));
        }


        builder.operations(op -> op.index(idx -> idx
            .index(INDEX_NAME)
            .id(camp.getContentId().toString())
            .document(doc)
        ));

      }

      log.info("총 {}개의 캠핑장 중, {}개의 데이터에 네이버 키워드가 성공적으로 저장되었습니다",allCampFromDB.size(), successfulJoins);

      esClient.bulk(builder.build());
      log.info("총 {}개의 캠핑장 데이터를 Elasticsearch에 성공적으로 동기화 완료", allCampFromDB.size());
    } catch ( Exception e) {
      log.error("동기화 작업 중 에러 발생", e);
    }
    log.info("====== 동기화 스케쥴러 종료 ======== ");
  }

  // CSV 파일을 클래스패스에서 안전하게 읽어 Map으로 만드는 헬퍼 메서드
  private Map<String, String> loadNaverKeywordsFromCsv(String filePath) throws IOException, CsvValidationException {
    Map<String, String> map = new HashMap<>();
    // ClassPathResource를 사용해 CSV 파일 찾기
    ClassPathResource resource = new ClassPathResource(filePath);

    try (InputStream inputStream = resource.getInputStream();
         CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      reader.readNext(); // 헤더라인 건너뛰기
      String[] line;
      while ((line = reader.readNext()) != null) {
        // CSV 파일이 (contentId, keyword) 순서 라고 가정
        if (line.length >= 2) {
          // Key: 캠핑장 이름 (line[0]), Value: 키워드 (line[1])
          try {
            map.put(line[0], line[1]);
          } catch (NumberFormatException e) {
            log.warn("CSV 파일의 캠핑장 이름 추출 과정에서 이상 발생 : {}", line[0]);
          }
        }
      }
    }
    return map;
  }

  //CampNm 객체를 엘라스틱서치에 저장할 DTO로 변환 후 데이터 가공 매서드
  private Map<String,Object> creatEsDocument(CampNm camp){
    Map<String, Object> doc = new HashMap<>();

    // DB에서 가져온 데이터 DTO로 셋팅
    doc.put("contentId", camp.getContentId());
    doc.put("facltNm", camp.getFacltNm());
    doc.put("doNm", camp.getDoNm());
    doc.put("firstImageUrl", camp.getFirstImageUrl());
    doc.put("addr1", camp.getAddr1());
    doc.put("lineIntro", camp.getLineIntro());
    doc.put("themaEnvrnCl", camp.getThemaEnvrnCl());

    if (camp.getColumn2() != null){
      doc.put("column2", camp.getColumn2().replace("\"", ""));
    }

    if (camp.getThemaEnvrnCl() != null){
      doc.put("themaEnvrnCl", camp.getThemaEnvrnCl());
    } else{
      doc.put("themaEnvrnCl", Collections.emptyList());
    }
    return doc;
  }

}
