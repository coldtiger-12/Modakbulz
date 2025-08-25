package modackbulz.app.Application.common;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexInitializer {

  private final ElasticsearchClient esClient;
  private final String INDEX_NAME = "camp_final";

  @PostConstruct  // 스프링 부트가 시작될때 마다 한번만 실행되도록 하는 애플리케이션
  public void initializeIndex(){
    try{
      boolean indexExists = esClient.indices().exists(r -> r
          .index(INDEX_NAME)).value();

      if (!indexExists){
        log.info("인덱스 '{}'가 존재하지 않습니다. 생성을 시작합니다.", INDEX_NAME);

        ClassPathResource resource = new ClassPathResource("elasticsearch/indices/" + INDEX_NAME + ".json");
        try (InputStream inputStream = resource.getInputStream()){

          esClient.indices().create(
               new CreateIndexRequest.Builder()
                  .index(INDEX_NAME)
                  .withJson(new java.io.StringReader(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)))
                  .build()
          );
              log.info("인덱스 '{}' 생성을 완료했습니다.", INDEX_NAME);
        }
      } else {
        log.info("인덱스 '{}'가 이미 존재하므로, 생성을 건너뜁니다.", INDEX_NAME);
      }
    } catch (Exception e) {
      log.error("인덱스 초기화 과정에서 에러가 발생했습니다.", e);
    }
  }
}
