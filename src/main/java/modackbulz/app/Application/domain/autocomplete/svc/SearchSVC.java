package modackbulz.app.Application.domain.autocomplete.svc;

import modackbulz.app.Application.domain.autocomplete.dto.AutocompleteDto;

import java.util.List;

public interface SearchSVC {

  List<AutocompleteDto.RegionResponse> getAutocompleteRegion(String keyword);

  List<AutocompleteDto.CampResponse> getAutocompleteCampsites(String keyword);

  List<AutocompleteDto.KeywordResponse> getAutocompleteKeywords(String keyword);

  List<String> getNaverKeywordForCamp(Long contentId);
}
