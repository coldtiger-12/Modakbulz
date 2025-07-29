package modackbulz.app.Application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class Application {

	public static void main(String[] args) {

		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		String hashedPassword = passwordEncoder.encode("Modakbulz123!"); // ⭐️ 확실한 비밀번호
		System.out.println("새로운 암호화된 비밀번호: " + hashedPassword);

		SpringApplication.run(Application.class, args);
	}
}