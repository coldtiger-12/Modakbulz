package modackbulz.app.Application.domain.camping.svc;

import lombok.extern.slf4j.Slf4j; // Slf4j 어노테이션 추가
import modackbulz.app.Application.domain.camping.dto.GoCampingDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j // 로그 사용을 위한 어노테이션 추가
@Service
public class GoCampingService {

  private final WebClient webClient;
  private final String serviceKey;

  public GoCampingService(@Value("${gocamping.api.service-key}") String serviceKey) {
    this.serviceKey = serviceKey;

    // URL 인코딩 문제를 해결하기 위한 설정
    DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory("http://apis.data.go.kr/B551011/GoCamping");
    factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

    this.webClient = WebClient.builder()
        .uriBuilderFactory(factory)
        .baseUrl("http://apis.data.go.kr/B551011/GoCamping")
        .build();
  }

  public Mono<List<GoCampingDto.Item>> getBasedList(int numOfRows) {
    String finalUrl = "/basedList?serviceKey=" + serviceKey + "&numOfRows=" + numOfRows + "&pageNo=1&MobileOS=ETC&MobileApp=Modakbulz&_type=json";
    log.info("Requesting GoCamping API: {}", finalUrl); // 요청 URL 로그 출력

    return webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/basedList")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", numOfRows)
            .queryParam("pageNo", 1)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "Modakbulz")
            .queryParam("_type", "json")
            .build())
        .retrieve()
        .bodyToMono(GoCampingDto.class)
        .map(dto -> {
          // API 응답 구조가 비어있는 경우를 대비한 null 체크
          if (dto.getResponse() == null || dto.getResponse().getBody() == null ||
              dto.getResponse().getBody().getItems() == null || dto.getResponse().getBody().getItems().getItem() == null) {
            log.warn("API response items are null.");
            return Collections.<GoCampingDto.Item>emptyList(); // 비어있는 리스트 반환
          }
          log.info("Successfully fetched {} items from GoCamping API.", dto.getResponse().getBody().getItems().getItem().size());
          return dto.getResponse().getBody().getItems().getItem();
        })
        .doOnError(error -> log.error("Error while calling GoCamping API", error)) // 에러 발생 시 로그 출력
        .onErrorReturn(Collections.emptyList()); // 에러 발생 시 비어있는 리스트를 반환하여 500 오류 방지
  }
}