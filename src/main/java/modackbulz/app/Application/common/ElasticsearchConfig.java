package modackbulz.app.Application.common;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "modackbulz.app.Application.domain.review.dao")
// 더 이상 ElasticsearchConfiguration을 상속받지 않게 끔 수정 (07-30)
// Spring Data Elasticsearch의 자동 설정 기능 사용 -> ElasticsearchClient를 직접 사용 방식
// 이유 : ElasticsearchClient를 직접 제어하고 주입받아 사용하기에는 적합하지 않을 수 있음
public class ElasticsearchConfig {

  // 수동으로 ElasticsearchClient 빈을 직접 정의하고 등록함
  @Bean
  public ElasticsearchClient elasticsearchClient() {
    // 로우 레벨 클라이언트 생성
    RestClient restClient = RestClient.builder(
        new HttpHost("localhost", 9200, "http")
    ).build();

    // 로우 레벨 클라이언트를 기반으로 Transport 객체 생성
    ElasticsearchTransport transport = new RestClientTransport(
        restClient, new JacksonJsonpMapper());

    // 최종 클라이언트 생성 및 반환
    return new ElasticsearchClient(transport);
  }
}