package modackbulz.app.Application.domain.camping.dto;

import lombok.Data;

@Data
public class CampingDataDto {
  private String facltNm; // 캠핑장명
  private String mapX;    // 경도
  private String mapY;    // 위도
  private String addr1;   // 주소
  private String addr2;   // 추가 주소 정보
  private String tel;     // 전화번호
}