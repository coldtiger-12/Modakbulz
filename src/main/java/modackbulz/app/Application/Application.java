package modackbulz.app.Application;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 👈 [추가] import

@SpringBootApplication
@EnableScheduling
@EnableEncryptableProperties	// Jasypt 활성화 어노테이션
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}