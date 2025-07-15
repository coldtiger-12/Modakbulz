package modackbulz.app.Application.web;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class VerificationCode {
  private String code;
  private LocalDateTime createdAt;

  public boolean isValid(String inputCode) {
    // 코드가 일치하고, 생성된 지 5분 이내인지 확인
    return this.code.equals(inputCode) && createdAt.plusMinutes(5).isAfter(LocalDateTime.now());
  }
}