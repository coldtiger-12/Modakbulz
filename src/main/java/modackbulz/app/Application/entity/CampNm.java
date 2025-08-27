package modackbulz.app.Application.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Elasticsearch 'camping_search' 인덱스의 문서를 매핑하기 위한 마스터 엔티티 클래스.
 * @JsonIgnoreProperties(ignoreUnknown = true) 어노테이션 덕분에,
 * ES에만 있고 이 클래스에는 없는 필드가 있어도 에러 없이 안전하게 파싱됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampNm {

  // --- 기본 정보 ---
  private Long contentId;
  private String facltNm;
  private String lineIntro;
  private String intro;
  private String featureNm;
  private String induty;
  private String firstImageUrl;

  // --- 위치 정보 ---
  private String doNm;
  private String sigunguNm;
  private String zipcode;
  private String addr1;
  private String addr2;
  private String direction;

  // --- 운영 정보 ---
  private String manageSttus;
  private String tel;
  private String homepage;
  private String operPdCl; // 운영기간
  private String operDeCl; // 운영일
  private String insrncAt; // 보험가입여부

  // --- 사이트 규모 정보 ---
  private Integer gnrlSiteCo; // 일반야영장 수
  private Integer autoSiteCo; // 자동차야영장 수
  private Integer glampSiteCo; // 글램핑 수
  private Integer caravSiteCo; // 카라반 수
  private Integer indvdlCaravSiteCo; // 개인카라반 수
  private Double sitedStnc; // 사이트간 거리

  // --- 부대시설 및 테마 정보 ---
  private String glampInnerFclty;
  private String caravInnerFclty;
  private String trlerAcmpnyAt; // 트레일러 동반 여부
  private String caravAcmpnyAt; // 카라반 동반 여부
  private String sbrsCl; // 부대시설
  private String themaEnvrnCl; // 테마
  private String eqpmnLendCl; // 장비대여
  private String animalCmgCl; // 반려동물 출입

  // --- 시간 정보 ---
  private String createdtime;
  private String modifiedtime;

  // --- 네이버 키워드 CSV에서 합쳐질 필드 ---
  // 이 필드는 DB에는 없지만, 동기화 과정에서 채워집니다.
  private String column2;
}
