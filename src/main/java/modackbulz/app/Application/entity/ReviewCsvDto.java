package modackbulz.app.Application.entity;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

@Data

public class ReviewCsvDto {

  // CSV 파일의 헤더 (첫 줄) 이름과 변수 이름을 매칭시켜줍니다.

  @CsvBindByPosition(position = 1)   // 두 번째 컬럼을 facltNm 필드에 매핑
  private String facltNm;

  @CsvBindByPosition(position = 5)   // 여섯번째 컬럼을 score 필드에 매핑
  private int score;

  @CsvBindByPosition(position = 4)   // 다섯번쨰 컬럼을 content 필드에 매핑
  private String content;

  // 필요하다면 CSV에 있는 다른 컬럼들도 추가 가능

}
