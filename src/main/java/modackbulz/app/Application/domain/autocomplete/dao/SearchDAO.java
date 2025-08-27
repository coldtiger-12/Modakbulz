package modackbulz.app.Application.domain.autocomplete.dao;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import modackbulz.app.Application.domain.autocomplete.dto.CampSearchDto;

import java.io.IOException;

public interface SearchDAO {
  SearchResponse<CampSearchDto> searchCampsitesByRegion(String keyword) throws IOException;
  SearchResponse<CampSearchDto> searchCampsitesByName(String keyword) throws IOException;
  SearchResponse<Void> aggregateKeywords(String keyword) throws IOException;
  CampSearchDto findCampById(String contentId) throws IOException;
}