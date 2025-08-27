package modackbulz.app.Application.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexInitializer {

  private final ElasticsearchClient esClient;
  private final List<String> indexNames = List.of("camping_search","search-logs");

  @PostConstruct
  public void initializeIndices() {
    for (String indexName : indexNames) {
      initializeIndex(indexName);
    }
  }

  private void initializeIndex(String indexName) {
    try {
      boolean indexExists = esClient.indices().exists(r -> r.index(indexName)).value();

      if (!indexExists) {
        log.info("인덱스 '{}'가 존재하지 않습니다. 생성을 시작합니다.", indexName);

        ClassPathResource resource = new ClassPathResource("elasticsearch/indices/" + indexName + ".json");
        if (resource.exists()) {
          // .json 파일이 있으면 파일 내용으로 인덱스 생성
          try (InputStream inputStream = resource.getInputStream()) {
            String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            esClient.indices().create(c -> c
                .index(indexName)
                .withJson(new StringReader(jsonContent))
            );
            log.info("인덱스 '{}' 생성을 완료했습니다. (from file)", indexName);
          }
        } else {
          log.warn("'{}'에 대한 .json 파일을 찾을 수 없습니다. 기본 매핑으로 인덱스를 생성합니다.", indexName);
          // .json 파일이 없으면 빈 인덱스 생성 (필요시 기본 매핑 추가 가능)
          esClient.indices().create(c -> c.index(indexName));
          log.info("인덱스 '{}' 생성을 완료했습니다. (default mapping)", indexName);
        }

      } else {
        log.info("인덱스 '{}'가 이미 존재하므로, 생성을 건너뜁니다.", indexName);
      }
    } catch (Exception e) {
      log.error("인덱스 '{}' 초기화 과정에서 에러가 발생했습니다.", indexName, e);
    }
  }
}