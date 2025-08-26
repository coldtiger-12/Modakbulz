package modackbulz.app.Application.web;

import modackbulz.app.Application.global.scheduler.WeatherDataScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/weather")
public class WeatherController {

  private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

  // API 키를 직접 설정
  private String apiKey = "GUOvwVNzuTSPApqy5naM3TznPDISb1DoxSBPofwaFsj2MU90mvfF1xlD1h4yASHvKGPEcAVNiDuUBhdc5A4Xmw==";

  private final RestTemplate restTemplate;
  private final WeatherDataScheduler weatherDataScheduler;

  public WeatherController(WeatherDataScheduler weatherDataScheduler, RestTemplate restTemplate) {
    this.weatherDataScheduler = weatherDataScheduler;
    this.restTemplate = restTemplate;
  }

  @GetMapping("/current")
  public ResponseEntity<Map<String, Object>> getCurrentWeather(@RequestParam("location") String location) {
    log.info("실시간 날씨 API 호출 시작 - 위치: {}", location);
    try {
      // 1) 캐시 먼저
      Map<String, Object> cachedData = weatherDataScheduler.getCachedWeatherData(location);
      if (cachedData != null) {
        log.info("캐시된 실시간 날씨 데이터 사용 - 위치: {}", location);
        return ResponseEntity.ok(cachedData);
      }

      // 2) 캐시가 없으면 실시간 호출로 대체
      return fetchLiveWeatherResponse(location);

    } catch (Exception e) {
      log.error("날씨 API 호출 실패: {}", e.getMessage(), e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "날씨 정보를 불러오는데 실패했습니다: " + e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
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

      // 실시간 관측 데이터 API URL
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
        log.info("실시간 관측 API 응답 바디: {}", response.getBody());
        Map<String, Object> currentWeather = processCurrentWeatherData(response.getBody());
        log.info("처리된 실시간 관측 데이터: {}", currentWeather);
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

  /**
   * 단기예보 데이터 가져오기
   */
  private Map<String, Object> fetchForecastWeather(String apiKey, int nx, int ny, String locationName) {
    try {
      String baseTime = getBaseTime();
      String baseDate = getCurrentDate();

      String encodedApiKey = java.net.URLEncoder.encode(apiKey, "UTF-8");

      String url = String.format(
          "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst?serviceKey=%s&pageNo=1&numOfRows=1000&dataType=JSON&base_date=%s&base_time=%s&nx=%d&ny=%d",
          encodedApiKey, baseDate, baseTime, nx, ny
      );

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<String> entity = new HttpEntity<>(headers);

      ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return processWeatherData(response.getBody(), locationName); // ← 제대로 전달
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
   * 좌표에 따른 관측소 ID 반환
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

  private String getCurrentDate() {
    // 현재 날짜를 YYYYMMDD 형식으로 반환
    LocalDate today = LocalDate.now();

    // 만약 현재 시간이 2시 45분 이전이라면 전날 날짜 사용
    LocalTime now = LocalTime.now();
    if (now.getHour() < 2 || (now.getHour() == 2 && now.getMinute() < 45)) {
      today = today.minusDays(1);
    }

    return today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
  }

  private String getBaseTime() {
    // 기상청 API 발표시각: 02시, 05시, 08시, 11시, 14시, 17시, 20시, 23시
    // 각 발표시각의 데이터는 발표시각 + 55분부터 사용 가능 (예: 14시 발표 → 14:55부터 사용)
    LocalTime now = LocalTime.now();
    int hour = now.getHour();
    int minute = now.getMinute();

    int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};
    int baseTime = 23; // 기본값

    // 현재 시간에 맞는 발표시각 찾기
    // 예: 현재 15:14 → 14시 발표시각 데이터 사용 (14:55부터 사용 가능)
    for (int i = baseTimes.length - 1; i >= 0; i--) {
      int bt = baseTimes[i];
      // 현재 시간이 발표시각 + 55분 이후라면 해당 발표시각 데이터 사용
      if (hour > bt || (hour == bt && minute >= 55)) {
        baseTime = bt;
        break;
      }
    }

    // 만약 현재 시간이 02:55 이전이라면 전날 23시 데이터 사용
    if (hour < 2 || (hour == 2 && minute < 55)) {
      baseTime = 23;
    }

    log.info("현재 시간: {}:{}, 선택된 base_time: {}", hour, minute, String.format("%02d00", baseTime));
    return String.format("%02d00", baseTime);
  }

  private Map<String, Object> processWeatherData(Map response, String location) {
    Map<String, Object> processedData = new HashMap<>();

    try {
      // 응답이 null인지 확인
      if (response == null) {
        log.error("기상청 API 응답이 null입니다.");
        throw new RuntimeException("기상청 API 응답이 null입니다.");
      }

      log.info("기상청 API 응답 구조: {}", response.keySet());
      log.info("기상청 API 전체 응답: {}", response);

      // 기상청 API 에러 응답 확인
      if (response.containsKey("cmmMsgHeader")) {
        log.error("기상청 API 에러 응답: {}", response);

        // 에러 메시지 추출 시도
        String errorMessage = "기상청 API 서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        try {
          Map<String, Object> cmmMsgHeader = (Map<String, Object>) response.get("cmmMsgHeader");
          if (cmmMsgHeader != null && cmmMsgHeader.containsKey("errMsg")) {
            errorMessage = "기상청 API 오류: " + cmmMsgHeader.get("errMsg");
          }
        } catch (Exception e) {
          log.warn("에러 메시지 추출 실패: {}", e.getMessage());
        }

        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", errorMessage);
        return errorData;
      }

      // 기상청 API 응답 구조에 맞게 데이터 파싱
      Map<String, Object> body = (Map<String, Object>) response.get("response");
      if (body == null) {
        log.error("기상청 API 응답에 'response' 필드가 없습니다. 전체 응답: {}", response);
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", "기상청 API 응답에 'response' 필드가 없습니다. 응답 구조: " + response.keySet());
        return errorData;
      }

      log.info("response.body 구조: {}", body.keySet());

      Map<String, Object> header = (Map<String, Object>) body.get("header");
      Map<String, Object> items = (Map<String, Object>) body.get("body");

      log.info("body.items 구조: {}", items != null ? items.keySet() : "null");

      if (items != null && items.get("items") != null) {
        Map<String, Object> itemsData = (Map<String, Object>) items.get("items");
        if (itemsData.get("item") != null) {
          // 현재 시간 기준으로 가장 가까운 예보 데이터 추출
          Map<String, Object> currentWeather = extractCurrentWeatherData((java.util.List) itemsData.get("item"));

          processedData.put("location", location);
          processedData.put("current", currentWeather);
          processedData.put("forecast", generateForecastData((java.util.List) itemsData.get("item")));
        } else {
          log.warn("기상청 API 응답에 'item' 데이터가 없습니다.");
          // 에러 정보를 Map으로 반환
          Map<String, Object> errorData = new HashMap<>();
          errorData.put("error", "기상청 API 응답에 'item' 데이터가 없습니다.");
          return errorData;
        }
      } else {
        log.warn("기상청 API 응답에 'items' 데이터가 없습니다.");
        // 에러 정보를 Map으로 반환
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", "기상청 API 응답에 'items' 데이터가 없습니다.");
        return errorData;
      }

    } catch (Exception e) {
      log.error("날씨 데이터 파싱 실패: {}", e.getMessage(), e);
      // 파싱 실패 시 에러 정보를 Map으로 반환
      Map<String, Object> errorData = new HashMap<>();
      errorData.put("error", "날씨 데이터 파싱에 실패했습니다: " + e.getMessage());
      return errorData;
    }

    return processedData;
  }

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
      log.info("현재 시간: {}, 선택된 예보 시간: {}", now, targetTime);

      for (Object item : items) {
        Map<String, Object> itemMap = (Map<String, Object>) item;
        String fcstTime = (String) itemMap.get("fcstTime");
        String category = (String) itemMap.get("category");
        String value = (String) itemMap.get("fcstValue");

        if (fcstTime != null && fcstTime.equals(targetTimeStr)) {
          log.info("매칭된 데이터: category={}, value={}, fcstTime={}", category, value, fcstTime);
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

    currentWeather.put("feels_like", (Integer) currentWeather.get("temp")); // 체감온도는 현재 기온으로 설정
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
   * 날씨 캐시 상태 확인 API
   */
  @GetMapping("/cache/status")
  public ResponseEntity<Map<String, Object>> getCacheStatus() {
    log.info("날씨 캐시 상태 확인 요청");

    try {
      Map<String, Object> cacheStatus = weatherDataScheduler.getCacheStatus();
      return ResponseEntity.ok(cacheStatus);
    } catch (Exception e) {
      log.error("캐시 상태 확인 중 오류 발생: {}", e.getMessage(), e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "캐시 상태 확인 중 오류가 발생했습니다: " + e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * 수동으로 캐시 업데이트 API (관리자용)
   */
  @PostMapping("/cache/update")
  public ResponseEntity<Map<String, Object>> updateCache() {
    log.info("수동 날씨 캐시 업데이트 요청");

    try {
      // 스케줄러의 일일 업데이트 메서드 호출
      weatherDataScheduler.updateWeatherCache();

      Map<String, Object> response = new HashMap<>();
      response.put("message", "날씨 캐시 업데이트가 시작되었습니다.");
      response.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("캐시 업데이트 중 오류 발생: {}", e.getMessage(), e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "캐시 업데이트 중 오류가 발생했습니다: " + e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * 강제 캐시 초기화 및 업데이트 API
   */
  @PostMapping("/cache/force-update")
  public ResponseEntity<Map<String, Object>> forceUpdateCache() {
    log.info("강제 날씨 캐시 업데이트 요청");

    try {
      // 캐시 초기화 및 강제 업데이트
      weatherDataScheduler.forceUpdateWeatherCache();

      Map<String, Object> response = new HashMap<>();
      response.put("message", "캐시 초기화 및 강제 업데이트가 완료되었습니다.");
      response.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("강제 캐시 업데이트 중 오류 발생: {}", e.getMessage(), e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "강제 캐시 업데이트 중 오류가 발생했습니다: " + e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * 실시간 날씨 데이터 강제 새로고침 API (캐시 무시)
   */
  @GetMapping("/current/fresh")
  public ResponseEntity<Map<String, Object>> getFreshCurrentWeather(
      @RequestParam("location") String location) {

    log.info("실시간 날씨 강제 새로고침 요청 - 위치: {}", location);

    try {
      // API 키 확인
      if (apiKey == null || apiKey.isEmpty()) {
        log.error("기상청 API 키가 설정되지 않았습니다.");
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "기상청 API 키가 설정되지 않았습니다.");
        return ResponseEntity.badRequest().body(errorResponse);
      }

      // 위치에 따른 좌표 설정
      int nx, ny;
      switch (location) {
        case "서울":
        case "서울특별시":
          nx = 60; ny = 127;
          break;
        case "부산":
        case "부산광역시":
          nx = 98; ny = 76;
          break;
        case "대구":
        case "대구광역시":
          nx = 89; ny = 90;
          break;
        case "인천":
        case "인천광역시":
          nx = 55; ny = 124;
          break;
        case "광주":
        case "광주광역시":
          nx = 58; ny = 74;
          break;
        case "대전":
        case "대전광역시":
          nx = 67; ny = 100;
          break;
        case "울산":
        case "울산광역시":
          nx = 102; ny = 84;
          break;
        case "세종":
        case "세종특별자치시":
          nx = 66; ny = 103;
          break;
        case "경기":
        case "경기도":
          nx = 60; ny = 120;
          break;
        case "강원":
        case "강원도":
          nx = 73; ny = 134;
          break;
        case "충북":
        case "충청북도":
          nx = 69; ny = 107;
          break;
        case "충남":
        case "충청남도":
          nx = 55; ny = 110;
          break;
        case "전북":
        case "전라북도":
          nx = 63; ny = 89;
          break;
        case "전남":
        case "전라남도":
          nx = 51; ny = 67;
          break;
        case "경북":
        case "경상북도":
          nx = 89; ny = 91;
          break;
        case "경남":
        case "경상남도":
          nx = 91; ny = 76;
          break;
        case "제주":
        case "제주특별자치도":
          nx = 53; ny = 38;
          break;
        default:
          nx = 55; ny = 127;
          break;
      }

      // 실시간 관측 데이터 먼저 시도
      Map<String, Object> currentWeather = fetchCurrentWeather(apiKey, nx, ny);
      Map<String, Object> forecastWeather = fetchForecastWeather(apiKey, nx, ny, location);

      log.info("실시간 관측 데이터 결과: {}", currentWeather);
      log.info("단기예보 데이터 결과: {}", forecastWeather);

      if (forecastWeather != null) {
        Map<String, Object> combinedData = new HashMap<>();
        combinedData.put("location", location);

        // 실시간 관측 데이터가 있으면 사용, 없으면 단기예보 데이터 사용
        if (currentWeather != null && !currentWeather.containsKey("error")) {
          combinedData.put("current", currentWeather);
          combinedData.put("data_type", "실시간관측");
          log.info("✅ {} 지역 실시간 관측 데이터 사용: {}", location, currentWeather);
        } else {
          // 단기예보에서 현재 시간에 가장 가까운 데이터 사용
          Map<String, Object> forecastCurrent = (Map<String, Object>) forecastWeather.get("current");
          if (forecastCurrent != null) {
            combinedData.put("current", forecastCurrent);
            combinedData.put("data_type", "단기예보");
            log.warn("⚠️ {} 지역 실시간 관측 데이터 실패, 단기예보 데이터 사용: {}", location, forecastCurrent);
          } else {
            log.error("❌ {} 지역 날씨 데이터 추출 실패", location);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "날씨 데이터를 가져올 수 없습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
          }
        }

        combinedData.put("forecast", forecastWeather.get("forecast"));
        combinedData.put("cached_at", System.currentTimeMillis());
        combinedData.put("fresh_data", true);
        return ResponseEntity.ok(combinedData);
      }

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "날씨 데이터를 가져올 수 없습니다.");
      return ResponseEntity.badRequest().body(errorResponse);

    } catch (Exception e) {
      log.error("날씨 API 호출 실패: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "날씨 정보를 불러오는데 실패했습니다: " + e.getMessage());

      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * 모든 지역 날씨 데이터 조회 API
   */
  @GetMapping("/all")
  public ResponseEntity<Map<String, Object>> getAllWeatherData() {
    log.info("모든 지역 날씨 데이터 조회 요청");

    try {
      Map<String, Object> allWeatherData = new HashMap<>();
      String[] cities = {"서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
          "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"};

      for (String city : cities) {
        Map<String, Object> weatherData = weatherDataScheduler.getCachedWeatherData(city);
        if (weatherData != null) {
          allWeatherData.put(city, weatherData);
        }
      }

      Map<String, Object> response = new HashMap<>();
      response.put("data", allWeatherData);
      response.put("total_cities", allWeatherData.size());
      response.put("timestamp", System.currentTimeMillis());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("모든 지역 날씨 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", "모든 지역 날씨 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  private ResponseEntity<Map<String, Object>> fetchLiveWeatherResponse(String location) {
    log.info("실시간(캐시무시) 날씨 요청 - 위치: {}", location);

    if (apiKey == null || apiKey.isEmpty()) {
      Map<String, Object> error = new HashMap<>();
      error.put("error", "기상청 API 키가 설정되지 않았습니다.");
      return ResponseEntity.badRequest().body(error);
    }

    // 위치 → 격자 nx, ny
    int nx, ny;
    switch (location) {
      case "서울":
      case "서울특별시": nx=60; ny=127; break;
      case "부산":
      case "부산광역시": nx=98; ny=76; break;
      case "대구":
      case "대구광역시": nx=89; ny=90; break;
      case "인천":
      case "인천광역시": nx=55; ny=124; break;
      case "광주":
      case "광주광역시": nx=58; ny=74; break;
      case "대전":
      case "대전광역시": nx=67; ny=100; break;
      case "울산":
      case "울산광역시": nx=102; ny=84; break;
      case "세종":
      case "세종특별자치시": nx=66; ny=103; break;
      case "경기":
      case "경기도": nx=60; ny=120; break;
      case "강원":
      case "강원도": nx=73; ny=134; break;
      case "충북":
      case "충청북도": nx=69; ny=107; break;
      case "충남":
      case "충청남도": nx=55; ny=110; break;
      case "전북":
      case "전라북도": nx=63; ny=89; break;
      case "전남":
      case "전라남도": nx=51; ny=67; break;
      case "경북":
      case "경상북도": nx=89; ny=91; break;
      case "경남":
      case "경상남도": nx=91; ny=76; break;
      case "제주":
      case "제주특별자치도": nx=53; ny=38; break;
      default: nx=60; ny=127; // 서울 기본
    }

    // 실시간 관측 + 단기예보 결합
    Map<String, Object> currentWeather = fetchCurrentWeather(apiKey, nx, ny);
    Map<String, Object> forecastWeather = fetchForecastWeather(apiKey, nx, ny, location); // ← (4)에서 시그니처 수정

    if (forecastWeather == null) {
      Map<String, Object> error = new HashMap<>();
      error.put("error", "날씨 데이터를 가져올 수 없습니다.");
      return ResponseEntity.badRequest().body(error);
    }

    Map<String, Object> combined = new HashMap<>();
    combined.put("location", location);

    if (currentWeather != null && !currentWeather.containsKey("error")) {
      combined.put("current", currentWeather);
      combined.put("data_type", "실시간관측");
    } else {
      Map<String, Object> forecastCurrent = (Map<String, Object>) forecastWeather.get("current");
      if (forecastCurrent == null) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "현재 날씨 데이터를 추출하지 못했습니다.");
        return ResponseEntity.badRequest().body(error);
      }
      combined.put("current", forecastCurrent);
      combined.put("data_type", "단기예보");
    }

    combined.put("forecast", forecastWeather.get("forecast"));
    combined.put("cached_at", System.currentTimeMillis());
    combined.put("fresh_data", true);
    return ResponseEntity.ok(combined);
  }
}