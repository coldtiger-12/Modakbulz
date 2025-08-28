package modackbulz.app.Application.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import modackbulz.app.Application.domain.camping.svc.GoCampingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampingDataScheduler {

  private final GoCampingService goCampingService;
  private static final int PAGE_SIZE = 100; // API 호출 시 한 번에 가져올 데이터 개수

  /**
   * 매일 새벽 4시에 GoCamping API 데이터를 동기화하는 스케줄러.
   * cron = "초 분 시 일 월 요일"
   * "0 0 4 * * ?" = 매일 새벽 4시 0분 0초에 실행
   */

  @Scheduled(cron = "0 37 14 * * ?")
  public void syncCampingData() {
    log.info("🏕️ 캠핑장 데이터 동기화 배치 작업을 시작합니다.");
    long startTime = System.currentTimeMillis();
    int currentPage = 0;
    int totalPages = 1; // 최소 1번은 실행되도록 초기값을 1로 설정
    int totalUpdatedCount = 0;

    // 전체 페이지를 순회하며 데이터 동기화
    while (currentPage < totalPages) {
      PageRequest pageRequest = PageRequest.of(currentPage, PAGE_SIZE);

      // GoCampingService를 통해 API 호출 및 DB 업데이트를 수행합니다.
      // .block()을 사용하여 비동기 호출 결과를 동기적으로 기다립니다.
      Page<GoCampingDto.Item> campPage = goCampingService.getCampListPage(pageRequest).block();

      if (campPage == null || !campPage.hasContent()) {
        log.info("API에서 더 이상 가져올 데이터가 없습니다. (페이지: {})", currentPage);
        break;
      }

      totalPages = campPage.getTotalPages(); // 전체 페이지 수 업데이트
      int fetchedCount = campPage.getNumberOfElements();
      totalUpdatedCount += fetchedCount;

      log.info("페이지 {}/{} 처리 완료. ({}개 항목 업데이트)", currentPage + 1, totalPages, fetchedCount);

      currentPage++;

      // API 서버 부하를 줄이기 위해 각 요청 사이에 잠시 대기합니다.
      try {
        Thread.sleep(500); // 0.5초 대기
      } catch (InterruptedException e) {
        log.error("배치 작업 중 스레드 대기 오류 발생", e);
        Thread.currentThread().interrupt(); // 인터럽트 상태를 다시 설정합니다.
      }
    }

    long endTime = System.currentTimeMillis();
    log.info("✅ 캠핑장 데이터 동기화 배치 작업을 완료했습니다. 총 {}개 항목 처리, 소요 시간: {}ms", totalUpdatedCount, (endTime - startTime));
  }
}