package modackbulz.app.Application.global.service;

import lombok.RequiredArgsConstructor;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class EncryptService {

  private final StringEncryptor stringEncryptor;

  // 암호화
  public String encrypt(String value) {
    if (value == null) {
      return null;
    }
    return stringEncryptor.encrypt(value);
  }

  // 복호화
  public String decrypt(String encryptedValue) {
    if (encryptedValue == null) {
      return null;
    }
    return stringEncryptor.decrypt(encryptedValue);
  }
}
