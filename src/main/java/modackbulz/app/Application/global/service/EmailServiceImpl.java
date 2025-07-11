package modackbulz.app.Application.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // 👈 [추가]
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender emailSender;
  private final String fromEmail; // 👈 [추가] 보내는 사람 이메일 주소를 담을 변수

  // 👇 [수정] 생성자에서 @Value를 통해 이메일 주입
  public EmailServiceImpl(JavaMailSender emailSender, @Value("${spring.mail.username}") String fromEmail) {
    this.emailSender = emailSender;
    this.fromEmail = fromEmail;
  }

  @Override
  public void sendEmail(String to, String subject, String text) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromEmail); // 👈 [수정] 하드코딩된 값 대신 주입받은 변수 사용
    message.setTo(to);
    message.setSubject(subject);
    message.setText(text);
    emailSender.send(message);
  }
}