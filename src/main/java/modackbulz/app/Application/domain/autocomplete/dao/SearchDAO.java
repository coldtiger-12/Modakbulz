package modackbulz.app.Application.domain.autocomplete.dao;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import modackbulz.app.Application.entity.CampNm;
import java.io.IOException;

public interface SearchDAO {
  SearchResponse<CampNm> searchCampsitesByRegion(String keyword) throws IOException;
  SearchResponse<CampNm> searchCampsitesByName(String keyword) throws IOException;
  SearchResponse<Void> aggregateKeywords(String keyword) throws IOException;
}