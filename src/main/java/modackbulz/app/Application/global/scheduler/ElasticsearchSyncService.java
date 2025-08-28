package modackbulz.app.Application.global.scheduler;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.autocomplete.dto.CampSearchDto;
import modackbulz.app.Application.domain.camping.dao.CampingDAO;
import modackbulz.app.Application.entity.CampNm;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncService {

  private final CampingDAO campingDAO;
  private final ElasticsearchClient esClient;
  private final String INDEX_NAME = "camping_search";

  /**
   * 매일 오후 12시에 실행 , DB의 모든 캠핑장 데이터를
   * Elasticesarch의 camping_search 인덱스로 동기화(덮어쓰기) 함
   */

  @Scheduled(cron = "0 37 14 * * *")
  public void syncCampsToElasticsearch(){
    log.info("======= DB & CSV -> Elasticsearch 동기화 스케쥴러 시작 ========");
    try{
      // 네이버 키워드 CSV를 먼저 읽어서 Map에 저장함 (contentId -> keyword)
      Map<String, List<String>> naverKeywordsMap = loadNaverKeywordsFromCsv("data/camping_keywords.csv");
      log.info("CSV에서 총 {}개의 네이버 키워드를 로드했습니다.", naverKeywordsMap.size());
      List<CampNm> allCampFromDB = campingDAO.findAllForSync();

      if (allCampFromDB == null || allCampFromDB.isEmpty()){
        log.warn("DB에 동기화할 캠핑장 데이터가 없습니다.");
        return;
      }

      BulkRequest.Builder builder = new BulkRequest.Builder();
      int successfulJoins = 0;

      for (CampNm camp : allCampFromDB){

        CampSearchDto esDoc = createEsDocument(camp, naverKeywordsMap);

        // 네이버 키워드가 성공적으로 연결 되었으면 카운트 수 증가
        if (esDoc.getColumn2() != null && !esDoc.getColumn2().isEmpty()){
          successfulJoins++;
        }

        builder.operations(op -> op.index(idx -> idx
            .index(INDEX_NAME)
            .id(camp.getContentId().toString())
            .document(esDoc)
        ));

      }

      BulkResponse result = esClient.bulk(builder.build());

      if (result.errors()){
        log.error("Elasticsearch 동기화 중 일부 데이터에서 오류가 발생했습니다.");
        for (BulkResponseItem item : result.items()){
          if (item.error() != null){
            log.error("ID {} : {}", item.id(), item.error().reason());
          }
        }
      } else {
        log.info("총 {}개의 캠핑장 데이터를 Elasticsearch에 성공적으로 동기화 완료", allCampFromDB.size());
        log.info("그 중 {}개의 데이터에 네이버 키워드가 연결되었습니다.", successfulJoins);
      }

    } catch ( Exception e) {
      log.error("동기화 작업 중 에러 발생", e);
    }
    log.info("====== 동기화 스케쥴러 종료 ======== ");
  }

  // CSV 파일을 클래스패스에서 안전하게 읽어 Map으로 만드는 헬퍼 메서드
  private Map<String, List<String>> loadNaverKeywordsFromCsv(String filePath) throws IOException, CsvValidationException {
    Map<String, List<String>> map = new HashMap<>();
    // ClassPathResource를 사용해 CSV 파일 찾기
    ClassPathResource resource = new ClassPathResource(filePath);

    try (InputStream inputStream = resource.getInputStream();
         CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      reader.readNext(); // 헤더라인 건너뛰기
      String[] line;
      while ((line = reader.readNext()) != null) {
        if (line.length >= 2 && line[0] != null && !line[0].isBlank()) {
          String campName = line[0];
          String keyword = line[1];

          // ' computeIfAbsent ' 를 사용해 지능적으로 관리
          // 만약 campName이라는 키가 없다면 , 새로운 빈 리스트를 생성 함
          // 그 후에 그 리스트에 새로운 키워드를 추가한다
          map.computeIfAbsent(campName, k -> new ArrayList<>()).add(keyword);
        }
      }
    }
    return map;
  }

  //CampNm 객체를 엘라스틱서치에 저장할 DTO로 변환 후 데이터 가공 매서드
  private CampSearchDto createEsDocument(CampNm camp, Map<String, List<String>> naverKeywordsMap){
    CampSearchDto esDoc = new CampSearchDto();

    // DB에서 가져온 데이터 DTO로 셋팅
    esDoc.setContentId(camp.getContentId());
    esDoc.setFacltNm(camp.getFacltNm());
    esDoc.setDoNm(camp.getDoNm());
    esDoc.setFirstImageUrl(camp.getFirstImageUrl());
    esDoc.setAddr1(camp.getAddr1());
    esDoc.setLineIntro(camp.getLineIntro());

    // 네이버 키워드 합치기 + 가공 (replace , split)
    List<String> keywords = naverKeywordsMap.get(camp.getFacltNm());
    if (keywords != null && !keywords.isEmpty()){

      List<String> cleanedAndDeduplicatedKeywords = keywords.stream()
          .filter(Objects::nonNull)
          .map(kw -> kw.replace("\"", ""))
          .collect(Collectors.toList())
          .stream()
          .collect(Collectors.toList());

      esDoc.setColumn2(cleanedAndDeduplicatedKeywords);
    } else {
      esDoc.setColumn2(Collections.emptyList());
    }

    // 테마 데이터 가공 (split)
    if (camp.getThemaEnvrnCl() != null && !camp.getThemaEnvrnCl().isEmpty()){
      esDoc.setThemaEnvrnCl(Arrays.asList(camp.getThemaEnvrnCl().split("\\s*,\\s*")));
    } else {
      esDoc.setThemaEnvrnCl(Collections.emptyList());
    }

    return esDoc;
  }

}
