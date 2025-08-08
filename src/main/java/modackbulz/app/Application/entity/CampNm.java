package modackbulz.app.Application.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// JSON에 있는데 우리 클래스엔 없는 필드가 있어도 무시하라는 어노테이션
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CampNm {

  // camp_2 인덱스의 필드 이름과 똑같이 변수를 만듬
  private String doNm;
  private String facltNm;
  private String lctCl;
  private String induty;

  // .. 이후 자동완성 기준점을 위한 필드들도 추가가 가능하다

}
