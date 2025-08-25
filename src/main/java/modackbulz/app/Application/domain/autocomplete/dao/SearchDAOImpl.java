package modackbulz.app.Application.domain.autocomplete.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.entity.CampNm;
import org.springframework.stereotype.Repository;

import java.io.IOException;

@Repository
@RequiredArgsConstructor
public class SearchDAOImpl implements SearchDAO {

  private final ElasticsearchClient esClient;
  private final String CAMP_INDEX = "camp_final";

  // 캠핑장 이름 (지역) 자동완성 - 지역 기반
  @Override
  public SearchResponse<CampNm> searchCampsitesByRegion(String keyword) throws IOException{
    return  esClient.search(s -> s
        .index(CAMP_INDEX)
        .query(q -> q
            .wildcard(w -> w
                .field("doNm")
                .value("*" + keyword + "*")
            )
        )
        .size(10),
        CampNm.class
    );

  }

  // 캠핑장 이름 자동완성
  @Override
  public SearchResponse<CampNm> searchCampsitesByName(String keyword) throws IOException {
    return esClient.search(s -> s
            .index(CAMP_INDEX)
            .query(q -> q
                .wildcard(w -> w
                    .field("facltNm")
                    .value("*" + keyword + "*")
                )
            )
            .size(100), // 서비스단에서 처리할 수 있도록 충분히 가져옵니다.
        CampNm.class
    );
  }

  // 키워드 자동완성
  @Override
  public SearchResponse<Void> aggregateKeywords(String keyword) throws IOException {
    String officialThemeAggName = "unique_official_themes";
    String keywordAggName = "unique_keyword_themes";

    return esClient.search(s -> s
            .index(CAMP_INDEX)
            .size(0)
            .query(q -> q
                .multiMatch(mm -> mm
                    .query(keyword)
                    .type(TextQueryType.BoolPrefix)
                    .fields("keyword_theme", "keyword_theme._2gram", "keyword_theme._3gram")
                )
            )
            .aggregations(officialThemeAggName, a -> a
                .terms(t -> t.field("themaEnvrnCl.keyword").size(10))
            )
            .aggregations(keywordAggName, a -> a
                .terms(t -> t.field("column2.keyword").size(10))
            ),
        Void.class
    );
  }
}
