package modackbulz.app.Application.web;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.camping.dto.CampingDataDto;
import modackbulz.app.Application.domain.camping.svc.CampingDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/camping")
public class CampingApiController {

  private final CampingDataService campingDataService;

  /**
   * xlsx 파일의 모든 캠핑장 데이터를 제공하는 API
   */
  @GetMapping("/all-data")
  public ResponseEntity<List<CampingDataDto>> getAllCampingData() {
    List<CampingDataDto> allCampingData = campingDataService.getAllCampingData();
    return ResponseEntity.ok(allCampingData);
  }
}