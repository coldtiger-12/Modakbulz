package modackbulz.app.Application.domain.popular.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import modackbulz.app.Application.domain.popular.dto.PopularSearchDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class PopularSearchDaoImpl implements PopularSearchDao {

  // 로깅을 위한 코드 추가
  private static final Logger log = LoggerFactory.getLogger(PopularSearchDaoImpl.class);

  private final ElasticsearchClient elasticsearchClient;

  public PopularSearchDaoImpl(ElasticsearchClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  @Override
  public void saveSearchKeyword(String keyword) {
    // 1. Elasticsearch에 저장할 데이터를 Map 형태로 직접 만듭니다.
    Map<String, Object> document = new HashMap<>();
    document.put("keyword", keyword); // 필드명 "keyword"는 매핑과 동일
    // 2. Kibana 매핑에 맞춰 필드명을 "searched_at"으로 정확하게 지정합니다.
    //    ZonedDateTime을 표준 형식(ISO_OFFSET_DATE_TIME)의 문자열로 변환합니다.
    document.put("searched_at", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    try {
      log.info(">>>>>>>>>> [ES 저장 시도] 데이터: {} <<<<<<<<<<", document);

      // 3. 이전처럼 객체 대신, 우리가 직접 만든 Map 데이터를 전송합니다.
      IndexResponse response = elasticsearchClient.index(i -> i
          .index("search-logs")
          .document(document)
      );

      log.info(">>>>>>>>>> [ES 저장 성공!] 결과: {}, ID: {} <<<<<<<<<<", response.result(), response.id());

    } catch (Exception e) {
      // 4. 실패할 경우, 어떤 에러가 발생했는지 로그에 자세히 남깁니다.
      log.error(">>>>>>>>>> [ES 저장 최종 실패] 원인: <<<<<<<<<<", e);
      throw new RuntimeException("Elasticsearch에 검색 키워드를 저장하지 못했습니다.", e);
    }
  }

  @Override
  public List<PopularSearchDto> getPopularSearches() {
    try {
      SearchResponse<Void> response = elasticsearchClient.search(s -> s
              .index("search-logs")
              .size(0)
              .aggregations("popular_keywords", a -> a
                  .terms(t -> t
                      // 필드명이 'keyword'이므로 그대로 사용
                      .field("keyword")
                      .size(10)
                  )
              ),
          Void.class
      );

      StringTermsAggregate agg = response.aggregations()
          .get("popular_keywords")
          .sterms();

      return agg.buckets().array().stream()
          .map(bucket -> new PopularSearchDto(bucket.key().stringValue(), bucket.docCount()))
          .collect(Collectors.toList());

    } catch (IOException e) {
      throw new RuntimeException("Failed to fetch popular searches", e);
    }
  }
}