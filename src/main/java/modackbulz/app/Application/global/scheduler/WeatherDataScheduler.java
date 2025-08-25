package modackbulz.app.Application.global.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WeatherDataScheduler {

  private final RestTemplate restTemplate;

  // 날씨 데이터 캐시 (메모리에 저장)
  private final Map<String, Map<String, Object>> weatherCache = new ConcurrentHashMap<>();

  public WeatherDataScheduler(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    updateWeatherCache();
  }

  // 주요 도시 목록
  private static final String[] MAJOR_CITIES = {
      "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
      "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
  };

  /**
   * 매일 새벽 3시에 전국 주요 도시의 날씨 데이터를 미리 요청하여 캐시에 저장
   */
  @Scheduled(cron = "0 0 3 * * ?")
  @Scheduled(cron = "0 */30 * * * ?")
  public void updateWeatherCache() {
    log.info("🌤️ 날씨 데이터 캐시 업데이트 배치 작업을 시작합니다.");
    long startTime = System.currentTimeMillis();
    int successCount = 0;
    int failCount = 0;

    for (String city : MAJOR_CITIES) {
      try {
        log.info("{} 지역 날씨 데이터 요청 중...", city);

        // 날씨 데이터 요청
        Map<String, Object> weatherData = fetchWeatherData(city);

        if (weatherData != null && !weatherData.containsKey("error")) {
          // 캐시에 저장
          weatherCache.put(city, weatherData);
          successCount++;
          log.info("✅ {} 지역 날씨 데이터 캐시 업데이트 완료", city);
        } else {
          failCount++;
          log.warn("⚠️ {} 지역 날씨 데이터 요청 실패", city);
        }

        // API 서버 부하를 줄이기 위해 잠시 대기
        Thread.sleep(1000); // 1초 대기

      } catch (Exception e) {
        failCount++;
        log.error("❌ {} 지역 날씨 데이터 요청 중 오류 발생: {}", city, e.getMessage());
      }
    }

    long endTime = System.currentTimeMillis();
    log.info("✅ 날씨 데이터 캐시 업데이트 완료. 성공: {}개, 실패: {}개, 소요시간: {}ms",
        successCount, failCount, (endTime - startTime));
  }

//    /**
//     * 30분마다 날씨 데이터 업데이트 (실시간성 향상)
//     */
//    @Scheduled(cron = "0 */30 * * * ?")
//    public void updateWeatherCacheHourly() {
//        log.info("🕐 30분마다 날씨 데이터 캐시 업데이트를 시작합니다.");
//
//        // 서울, 부산, 대구 등 주요 5개 도시만 30분마다 업데이트
////        String[] priorityCities = {"서울", "부산", "대구", "인천", "경기"};
//
//        for (String city : MAJOR_CITIES) {
//            try {
//                Map<String, Object> weatherData = fetchWeatherData(city);
//                if (weatherData != null && !weatherData.containsKey("error")) {
//                    weatherCache.put(city, weatherData);
//                    log.info("✅ {} 지역 시간별 날씨 데이터 업데이트 완료", city);
//                }
//                Thread.sleep(500); // 0.5초 대기
//            } catch (Exception e) {
//                log.error("❌ {} 지역 시간별 날씨 데이터 업데이트 실패: {}", city, e.getMessage());
//            }
//        }
//    }

  /**
   * 캐시된 날씨 데이터 조회
   */
  public Map<String, Object> getCachedWeatherData(String city) {
    return weatherCache.get(city);
  }

  /**
   * 캐시 초기화 및 강제 업데이트
   */
  public void forceUpdateWeatherCache() {
    log.info("🔄 강제 날씨 캐시 업데이트 시작");

    // 캐시 완전 초기화
    weatherCache.clear();
    log.info("캐시 초기화 완료");

    // 모든 주요 도시 업데이트
    int successCount = 0;
    int failCount = 0;

    for (String city : MAJOR_CITIES) {
      try {
        log.info("{} 지역 강제 업데이트 시작", city);
        Map<String, Object> weatherData = fetchWeatherData(city);

        if (weatherData != null && !weatherData.containsKey("error")) {
          weatherCache.put(city, weatherData);
          successCount++;
          log.info("✅ {} 지역 강제 업데이트 성공: {}", city, weatherData.get("data_type"));
        } else {
          failCount++;
          log.error("❌ {} 지역 강제 업데이트 실패: {}", city, weatherData);
        }

        // API 서버 부하를 줄이기 위해 잠시 대기
        Thread.sleep(500); // 0.5초 대기

      } catch (Exception e) {
        failCount++;
        log.error("❌ {} 지역 강제 업데이트 중 오류: {}", city, e.getMessage());
      }
    }

    log.info("🔄 강제 날씨 캐시 업데이트 완료. 성공: {}개, 실패: {}개", successCount, failCount);
  }

  /**
   * 캐시 상태 확인
   */
  public Map<String, Object> getCacheStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("totalCities", MAJOR_CITIES.length);
    status.put("cachedCities", weatherCache.size());
    status.put("cachedCitiesList", weatherCache.keySet());
    status.put("lastUpdate", LocalDate.now().toString() + " " + LocalTime.now().toString());
    return status;
  }

  /**
   * 기상청 API에서 날씨 데이터 가져오기 (실시간 관측 데이터 우선)
   */
  private Map<String, Object> fetchWeatherData(String city) {
    try {
      // API 키
      String apiKey = "GUOvwVNzuTSPApqy5naM3TznPDISb1DoxSBPofwaFsj2MU90mvfF1xlD1h4yASHvKGPEcAVNiDuUBhdc5A4Xmw==";

      // 도시별 좌표 설정
      int[] coordinates = getCityCoordinates(city);
      int nx = coordinates[0];
      int ny = coordinates[1];

      // 실시간 관측 데이터 먼저 시도
//            Map<String, Object> currentWeather = fetchCurrentWeather(apiKey, nx, ny);
      Map<String, Object> currentWeather = null;
      // 단기예보
      Map<String, Object> forecastWeather = fetchForecastWeather(apiKey, nx, ny);

      if (forecastWeather != null) {
        Map<String, Object> combinedData = new HashMap<>();
        combinedData.put("location", city);

        // 실시간 관측 데이터가 있으면 사용, 없으면 단기예보 데이터 사용
        if (currentWeather != null && !currentWeather.containsKey("error")) {
          combinedData.put("current", currentWeather);
          combinedData.put("data_type", "실시간관측");
          log.info("✅ {} 지역 실시간 관측 데이터 사용", city);
        } else {
          // 단기예보에서 현재 시간에 가장 가까운 데이터 사용
          Map<String, Object> forecastCurrent = (Map<String, Object>) forecastWeather.get("current");
          if (forecastCurrent != null) {
            combinedData.put("current", forecastCurrent);
            combinedData.put("data_type", "단기예보");
            log.warn("⚠️ {} 지역 실시간 관측 데이터 실패, 단기예보 데이터 사용", city);
          } else {
            log.error("❌ {} 지역 날씨 데이터 추출 실패", city);
            return null;
          }
        }

        combinedData.put("forecast", forecastWeather.get("forecast"));
        combinedData.put("cached_at", System.currentTimeMillis());
        return combinedData;
      }

      return null;

    } catch (Exception e) {
      log.error("날씨 데이터 요청 중 오류 발생: {}", e.getMessage());
      return null;
    }
  }



  /**
   * 단기예보 데이터 가져오기
   */
  private Map<String, Object> fetchForecastWeather(String apiKey, int nx, int ny) {
    try {
      // 현재 시간에 맞는 base_time과 base_date 계산
      String baseTime = getBaseTime();
      String baseDate = getCurrentDate();

      // 단기예보 API URL
      String url = String.format(
          "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst?serviceKey=%s&pageNo=1&numOfRows=1000&dataType=JSON&base_date=%s&base_time=%s&nx=%d&ny=%d",
          apiKey, baseDate, baseTime, nx, ny
      );

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return processWeatherData(response.getBody(), "forecast");
      } else {
        log.error("단기예보 API 응답 오류: {}", response.getStatusCode());
        return null;
      }

    } catch (Exception e) {
      log.error("단기예보 데이터 요청 중 오류: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 현재 날짜를 YYYYMMDD 형식으로 반환
   */
  private String getCurrentDate() {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();

    // 만약 현재 시간이 2시 45분 이전이라면 전날 날짜 사용
    if (now.getHour() < 2 || (now.getHour() == 2 && now.getMinute() < 45)) {
      today = today.minusDays(1);
    }

    return today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
  }

  /**
   * 기상청 API 발표시각 계산
   */
  private String getBaseTime() {
    // 기상청 API 발표시각: 02시, 05시, 08시, 11시, 14시, 17시, 20시, 23시
    // 각 발표시각의 데이터는 발표시각 + 10분부터 사용 가능 (예: 14시 발표 → 14:10부터 사용)
    LocalTime now = LocalTime.now();
    int hour = now.getHour();
    int minute = now.getMinute();

    int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};
    int baseTime = 23; // 기본값

    // 현재 시간에 맞는 발표시각 찾기
    // 예: 현재 15:14 → 14시 발표시각 데이터 사용 (14:55부터 사용 가능)
    for (int i = baseTimes.length - 1; i >= 0; i--) {
      int bt = baseTimes[i];
      // 현재 시간이 발표시각 + 10분 이후라면 해당 발표시각 데이터 사용
      if (hour > bt || (hour == bt && minute >= 10)) {
        baseTime = bt;
        break;
      }
    }

    // 만약 현재 시간이 02:55 이전이라면 전날 23시 데이터 사용
    if (hour < 2 || (hour == 2 && minute < 10)) {
      baseTime = 23;
    }

    return String.format("%02d00", baseTime);
  }

  /**
   * 도시별 좌표 반환
   */
  private int[] getCityCoordinates(String city) {
    switch (city) {
      case "서울": return new int[]{60, 127}; // 서울시청 좌표
      case "부산": return new int[]{98, 76};
      case "대구": return new int[]{89, 90};
      case "인천": return new int[]{55, 124};
      case "광주": return new int[]{58, 74};
      case "대전": return new int[]{67, 100};
      case "울산": return new int[]{102, 84};
      case "세종": return new int[]{66, 103};
      case "경기": return new int[]{60, 120};
      case "강원": return new int[]{73, 134};
      case "충북": return new int[]{69, 107};
      case "충남": return new int[]{55, 110};
      case "전북": return new int[]{63, 89};
      case "전남": return new int[]{51, 67};
      case "경북": return new int[]{89, 91};
      case "경남": return new int[]{91, 76};
      case "제주": return new int[]{53, 38};
      default: return new int[]{60, 127}; // 서울시청 기본값
    }
  }

  /**
   * 기상청 API 응답 데이터 처리
   */
  private Map<String, Object> processWeatherData(Map response, String city) {
    try {
      if (response == null) {
        return null;
      }

      // 기상청 API 에러 응답 확인
      if (response.containsKey("cmmMsgHeader")) {
        log.error("기상청 API 에러 응답: {}", response);
        return null;
      }

      Map<String, Object> body = (Map<String, Object>) response.get("response");
      if (body == null) {
        return null;
      }

      Map<String, Object> items = (Map<String, Object>) body.get("body");
      if (items != null && items.get("items") != null) {
        Map<String, Object> itemsData = (Map<String, Object>) items.get("items");
        if (itemsData.get("item") != null) {
          Map<String, Object> currentWeather = extractCurrentWeatherData((java.util.List) itemsData.get("item"));

          Map<String, Object> processedData = new HashMap<>();
          processedData.put("location", city);
          processedData.put("current", currentWeather);
          processedData.put("forecast", generateForecastData((java.util.List) itemsData.get("item")));
          processedData.put("cached_at", System.currentTimeMillis());

          return processedData;
        }
      }

    } catch (Exception e) {
      log.error("날씨 데이터 파싱 실패: {}", e.getMessage());
    }

    return null;
  }

  /**
   * 현재 날씨 데이터 추출 (예보 데이터에서 현재 시간에 가장 가까운 데이터 선택)
   */
  private Map<String, Object> extractCurrentWeatherData(java.util.List items) {
    Map<String, Object> currentWeather = new HashMap<>();

    LocalTime now = LocalTime.now();
    LocalTime targetTime = null;
    long minTimeDiff = Long.MAX_VALUE;

    log.info("현재 시간: {}, 예보 데이터에서 가장 가까운 시간 찾기 시작", now);

    // 현재 시간에 가장 가까운 예보 시간 찾기
    for (Object item : items) {
      Map<String, Object> itemMap = (Map<String, Object>) item;
      String fcstTime = (String) itemMap.get("fcstTime");

      if (fcstTime != null && fcstTime.length() >= 4) {
        try {
          int hour = Integer.parseInt(fcstTime.substring(0, 2));
          int minute = Integer.parseInt(fcstTime.substring(2, 4));
          LocalTime forecastTime = LocalTime.of(hour, minute);

          // 현재 시간과의 차이 계산
          long timeDiff = Math.abs(java.time.Duration.between(now, forecastTime).toMinutes());

          log.debug("예보 시간: {}, 현재 시간과의 차이: {}분", forecastTime, timeDiff);

          if (timeDiff < minTimeDiff) {
            minTimeDiff = timeDiff;
            targetTime = forecastTime;
          }
        } catch (NumberFormatException e) {
          // 시간 파싱 실패 시 무시
        }
      }
    }

    // 가장 가까운 시간의 데이터 추출
    if (targetTime != null) {
      String targetTimeStr = String.format("%02d%02d", targetTime.getHour(), targetTime.getMinute());

      for (Object item : items) {
        Map<String, Object> itemMap = (Map<String, Object>) item;
        String fcstTime = (String) itemMap.get("fcstTime");
        String category = (String) itemMap.get("category");
        String value = (String) itemMap.get("fcstValue");

        if (fcstTime != null && fcstTime.equals(targetTimeStr)) {
          switch (category) {
            case "TMP": // 기온
              currentWeather.put("temp", Integer.parseInt(value));
              break;
            case "REH": // 습도
              currentWeather.put("humidity", Integer.parseInt(value));
              break;
            case "WSD": // 풍속
              currentWeather.put("wind_speed", Double.parseDouble(value));
              break;
            case "SKY": // 하늘상태
              currentWeather.put("weather_code", getWeatherCodeFromSky(value));
              currentWeather.put("description", getWeatherDescriptionFromSky(value));
              break;
          }
        }
      }
    }

    // 기본값 설정
    if (!currentWeather.containsKey("temp")) currentWeather.put("temp", 20);
    if (!currentWeather.containsKey("humidity")) currentWeather.put("humidity", 60);
    if (!currentWeather.containsKey("wind_speed")) currentWeather.put("wind_speed", 3.0);
    if (!currentWeather.containsKey("weather_code")) currentWeather.put("weather_code", "01");
    if (!currentWeather.containsKey("description")) currentWeather.put("description", "맑음");

    currentWeather.put("feels_like", (Integer) currentWeather.get("temp"));
    currentWeather.put("data_source", "단기예보");
    currentWeather.put("forecast_time", targetTime != null ? targetTime.toString() : "알 수 없음");

    return currentWeather;
  }

  private String getWeatherCodeFromSky(String skyValue) {
    switch (skyValue) {
      case "1": return "01"; // 맑음
      case "3": return "02"; // 구름조금
      case "4": return "03"; // 구름많음
      default: return "01";
    }
  }

  private String getWeatherDescriptionFromSky(String skyValue) {
    switch (skyValue) {
      case "1": return "맑음";
      case "3": return "구름조금";
      case "4": return "구름많음";
      default: return "맑음";
    }
  }

  private java.util.List<Map<String, Object>> generateForecastData(java.util.List items) {
    java.util.List<Map<String, Object>> forecast = new java.util.ArrayList<>();

    // 실제 API 데이터에서 예보 정보 추출
    Map<String, Map<String, Object>> timeDataMap = new HashMap<>();

    // API 데이터에서 시간별 예보 정보 수집
    for (Object item : items) {
      Map<String, Object> itemMap = (Map<String, Object>) item;
      String fcstTime = (String) itemMap.get("fcstTime");
      String category = (String) itemMap.get("category");
      String value = (String) itemMap.get("fcstValue");

      if (fcstTime != null && fcstTime.length() >= 4) {
        String timeKey = fcstTime.substring(0, 2) + ":" + fcstTime.substring(2, 4);

        // 해당 시간의 데이터가 없으면 새로 생성
        if (!timeDataMap.containsKey(timeKey)) {
          Map<String, Object> timeData = new HashMap<>();
          timeData.put("time", timeKey);
          timeData.put("temp", 20); // 기본값
          timeData.put("weather_code", "01"); // 기본값
          timeData.put("description", "맑음"); // 기본값
          timeData.put("humidity", 60); // 기본값
          timeData.put("wind_speed", 3.0); // 기본값
          timeDataMap.put(timeKey, timeData);
        }

        Map<String, Object> timeData = timeDataMap.get(timeKey);

        // 카테고리별 데이터 설정
        switch (category) {
          case "TMP": // 기온
            timeData.put("temp", Integer.parseInt(value));
            break;
          case "REH": // 습도
            timeData.put("humidity", Integer.parseInt(value));
            break;
          case "WSD": // 풍속
            timeData.put("wind_speed", Double.parseDouble(value));
            break;
          case "SKY": // 하늘상태
            timeData.put("weather_code", getWeatherCodeFromSky(value));
            timeData.put("description", getWeatherDescriptionFromSky(value));
            break;
        }
      }
    }

    // 현재 시간 이후의 예보만 필터링하고 시간순으로 정렬
    LocalTime now = LocalTime.now();
    java.util.List<String> sortedTimes = new java.util.ArrayList<>();

    for (String timeKey : timeDataMap.keySet()) {
      try {
        String[] timeParts = timeKey.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        LocalTime forecastTime = LocalTime.of(hour, minute);

        // 현재 시간보다 미래의 예보만 포함
        if (forecastTime.isAfter(now)) {
          sortedTimes.add(timeKey);
        }
      } catch (Exception e) {
        log.warn("시간 파싱 실패: {}", timeKey);
      }
    }

    // 시간순 정렬
    sortedTimes.sort((t1, t2) -> {
      try {
        String[] parts1 = t1.split(":");
        String[] parts2 = t2.split(":");
        int hour1 = Integer.parseInt(parts1[0]);
        int hour2 = Integer.parseInt(parts2[0]);
        int minute1 = Integer.parseInt(parts1[1]);
        int minute2 = Integer.parseInt(parts2[1]);

        if (hour1 != hour2) {
          return Integer.compare(hour1, hour2);
        }
        return Integer.compare(minute1, minute2);
      } catch (Exception e) {
        return 0;
      }
    });

    // 최대 8개의 예보 데이터만 반환 (3시간 간격으로)
    int count = 0;
    for (String timeKey : sortedTimes) {
      if (count >= 8) break;

      Map<String, Object> timeData = timeDataMap.get(timeKey);
      if (timeData != null) {
        forecast.add(timeData);
        count++;
      }
    }

    // 예보 데이터가 부족한 경우 기본 데이터 추가
    if (forecast.isEmpty()) {
      String[] defaultTimes = {"15:00", "18:00", "21:00", "내일 09:00"};
      for (String time : defaultTimes) {
        Map<String, Object> forecastItem = new HashMap<>();
        forecastItem.put("time", time);
        forecastItem.put("temp", 20);
        forecastItem.put("weather_code", "01");
        forecastItem.put("description", "맑음");
        forecastItem.put("humidity", 60);
        forecastItem.put("wind_speed", 3.0);
        forecast.add(forecastItem);
      }
    }

    log.info("생성된 예보 데이터: {}개", forecast.size());
    return forecast;
  }

  /**
   * 실시간 관측 데이터 처리
   */
  private Map<String, Object> processCurrentWeatherData(Map response) {
    try {
      if (response == null) {
        log.error("실시간 관측 API 응답이 null입니다");
        return null;
      }

      // 기상청 API 에러 응답 확인
      if (response.containsKey("cmmMsgHeader")) {
        log.error("실시간 관측 API 에러 응답: {}", response);
        return null;
      }

      Map<String, Object> body = (Map<String, Object>) response.get("response");
      if (body == null) {
        log.error("실시간 관측 API response.body가 null입니다");
        return null;
      }

      Map<String, Object> items = (Map<String, Object>) body.get("body");
      if (items != null && items.get("items") != null) {
        Map<String, Object> itemsData = (Map<String, Object>) items.get("items");
        if (itemsData.get("item") != null) {
          java.util.List itemList = (java.util.List) itemsData.get("item");
          if (!itemList.isEmpty()) {
            // 가장 최근 관측 데이터 사용 (시간순 정렬)
            Map<String, Object> latestItem = (Map<String, Object>) itemList.get(0);
            log.info("실시간 관측 데이터 추출 성공: {}", latestItem);
            return extractCurrentWeatherFromObservation(latestItem);
          } else {
            log.error("실시간 관측 데이터 item 리스트가 비어있습니다");
          }
        } else {
          log.error("실시간 관측 데이터 items.item이 null입니다");
        }
      } else {
        log.error("실시간 관측 데이터 body.items가 null입니다");
      }

    } catch (Exception e) {
      log.error("실시간 관측 데이터 파싱 실패: {}", e.getMessage());
    }

    return null;
  }

  /**
   * 실시간 관측 데이터에서 현재 날씨 추출
   */
  private Map<String, Object> extractCurrentWeatherFromObservation(Map<String, Object> item) {
    Map<String, Object> currentWeather = new HashMap<>();

    try {
      // 기온 (ta: 기온)
      String temp = (String) item.get("ta");
      if (temp != null && !temp.isEmpty() && !temp.equals("-999")) {
        currentWeather.put("temp", Integer.parseInt(temp));
      } else {
        currentWeather.put("temp", 20);
      }

      // 습도 (hm: 습도)
      String humidity = (String) item.get("hm");
      if (humidity != null && !humidity.isEmpty() && !humidity.equals("-999")) {
        currentWeather.put("humidity", Integer.parseInt(humidity));
      } else {
        currentWeather.put("humidity", 60);
      }

      // 풍속 (ws: 풍속)
      String windSpeed = (String) item.get("ws");
      if (windSpeed != null && !windSpeed.isEmpty() && !windSpeed.equals("-999")) {
        currentWeather.put("wind_speed", Double.parseDouble(windSpeed));
      } else {
        currentWeather.put("wind_speed", 3.0);
      }

      // 기압 (pa: 기압)
      String pressure = (String) item.get("pa");
      if (pressure != null && !pressure.isEmpty() && !pressure.equals("-999")) {
        currentWeather.put("pressure", Double.parseDouble(pressure));
      }

      // 하늘상태 (관측 데이터에는 없으므로 기본값)
      currentWeather.put("weather_code", "01");
      currentWeather.put("description", "맑음");

      // 체감온도 (기온과 동일하게 설정)
      currentWeather.put("feels_like", currentWeather.get("temp"));

      // 관측 시간 (tm: 관측시간)
      String obsTime = (String) item.get("tm");
      if (obsTime != null) {
        currentWeather.put("observation_time", obsTime);
      }

      // 관측소 정보
      String stationName = (String) item.get("stnNm");
      if (stationName != null) {
        currentWeather.put("station_name", stationName);
      }

    } catch (Exception e) {
      log.error("관측 데이터 파싱 중 오류: {}", e.getMessage());
      return null;
    }

    return currentWeather;
  }

  /**
   * 기본 현재 날씨 데이터 생성
   */
  private Map<String, Object> generateDefaultCurrentWeather() {
    Map<String, Object> currentWeather = new HashMap<>();
    currentWeather.put("temp", 20);
    currentWeather.put("humidity", 60);
    currentWeather.put("wind_speed", 3.0);
    currentWeather.put("weather_code", "01");
    currentWeather.put("description", "맑음");
    currentWeather.put("feels_like", 20);
    currentWeather.put("observation_time", "기본값");
    return currentWeather;
  }

  /**
   * 기본 예보 데이터 생성
   */
  private java.util.List<Map<String, Object>> generateDefaultForecast() {
    java.util.List<Map<String, Object>> forecast = new java.util.ArrayList<>();
    String[] times = {"15:00", "18:00", "21:00", "내일 09:00"};

    for (String time : times) {
      Map<String, Object> forecastItem = new HashMap<>();
      forecastItem.put("time", time);
      forecastItem.put("temp", 20);
      forecastItem.put("weather_code", "01");
      forecast.add(forecastItem);
    }

    return forecast;
  }

  /**
   * 좌표에 따른 관측소 ID 반환 (기상청 ASOS 관측소)
   */
  private String getStationId(int nx, int ny) {
    // 서울시청 근처 관측소 (108번 관측소 - 서울)
    if (nx >= 55 && nx <= 65 && ny >= 125 && ny <= 130) {
      return "108";
    }
    // 부산 관측소 (159번 - 부산)
    else if (nx >= 95 && nx <= 105 && ny >= 70 && ny <= 80) {
      return "159";
    }
    // 대구 관측소 (143번 - 대구)
    else if (nx >= 85 && nx <= 95 && ny >= 85 && ny <= 95) {
      return "143";
    }
    // 인천 관측소 (112번 - 인천)
    else if (nx >= 50 && nx <= 60 && ny >= 120 && ny <= 130) {
      return "112";
    }
    // 광주 관측소 (156번 - 광주)
    else if (nx >= 55 && nx <= 65 && ny >= 70 && ny <= 80) {
      return "156";
    }
    // 대전 관측소 (133번 - 대전)
    else if (nx >= 65 && nx <= 75 && ny >= 95 && ny <= 105) {
      return "133";
    }
    // 울산 관측소 (152번 - 울산)
    else if (nx >= 100 && nx <= 110 && ny >= 80 && ny <= 90) {
      return "152";
    }
    // 세종 관측소 (177번 - 세종)
    else if (nx >= 60 && nx <= 70 && ny >= 100 && ny <= 110) {
      return "177";
    }
    // 수원 관측소 (119번 - 경기)
    else if (nx >= 55 && nx <= 65 && ny >= 115 && ny <= 125) {
      return "119";
    }
    // 강릉 관측소 (105번 - 강원)
    else if (nx >= 70 && nx <= 80 && ny >= 130 && ny <= 140) {
      return "105";
    }
    // 청주 관측소 (131번 - 충북)
    else if (nx >= 65 && nx <= 75 && ny >= 105 && ny <= 115) {
      return "131";
    }
    // 대천 관측소 (235번 - 충남)
    else if (nx >= 50 && nx <= 60 && ny >= 105 && ny <= 115) {
      return "235";
    }
    // 전주 관측소 (146번 - 전북)
    else if (nx >= 60 && nx <= 70 && ny >= 85 && ny <= 95) {
      return "146";
    }
    // 목포 관측소 (165번 - 전남)
    else if (nx >= 45 && nx <= 55 && ny >= 65 && ny <= 75) {
      return "165";
    }
    // 포항 관측소 (138번 - 경북)
    else if (nx >= 90 && nx <= 100 && ny >= 85 && ny <= 95) {
      return "138";
    }
    // 창원 관측소 (155번 - 경남)
    else if (nx >= 85 && nx <= 95 && ny >= 70 && ny <= 80) {
      return "155";
    }
    // 제주 관측소 (184번 - 제주)
    else if (nx >= 50 && nx <= 60 && ny >= 35 && ny <= 45) {
      return "184";
    }
    // 기본값 (서울)
    else {
      return "108";
    }
  }

  /**
   * 실시간 관측 데이터 가져오기
   */
  private Map<String, Object> fetchCurrentWeather(String apiKey, int nx, int ny) {
    try {
      String stationId = getStationId(nx, ny);
      String currentDate = getCurrentDate();

      // API 키 URL 인코딩
      String encodedApiKey = java.net.URLEncoder.encode(apiKey, "UTF-8");

      // 실시간 관측 데이터 API URL (기상청 ASOS 시간별 관측 데이터)
      String url = String.format(
          "http://apis.data.go.kr/1360000/AsosHourlyInfoService/getAsosHourlyInfo?serviceKey=%s&pageNo=1&numOfRows=24&dataType=JSON&dataCd=ASOS&dateCd=HR&startDt=%s&endDt=%s&stnIds=%s",
          encodedApiKey,
          currentDate,
          currentDate,
          stationId
      );

      log.info("실시간 관측 데이터 요청 - 관측소: {}, 날짜: {}", stationId, currentDate);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        log.info("실시간 관측 API 응답 성공");
        Map<String, Object> currentWeather = processCurrentWeatherData(response.getBody());
        if (currentWeather != null) {
          currentWeather.put("data_source", "실시간관측");
          currentWeather.put("station_id", stationId);
        }
        return currentWeather;
      } else {
        log.error("실시간 관측 API 응답 오류: {} - {}", response.getStatusCode(), response.getBody());
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", "실시간 관측 API 실패: " + response.getStatusCode());
        return errorData;
      }

    } catch (Exception e) {
      log.error("실시간 관측 데이터 요청 중 오류: {}", e.getMessage());
      Map<String, Object> errorData = new HashMap<>();
      errorData.put("error", "실시간 관측 API 예외: " + e.getMessage());
      return errorData;
    }
  }


}
