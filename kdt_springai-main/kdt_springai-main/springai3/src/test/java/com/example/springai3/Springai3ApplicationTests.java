package com.example.springai3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;


//@SpringBootTest
class Springai3ApplicationTests {
	@Test
	void recordTest1() {
		Person person = new Person("둘리", 10);

		// 필드는 private final로 정의되므로 직접 접근 불가
		// person.name = "유니코"; // 컴파일 에러: final 필드이므로 값 변경 불가
		// System.out.println(person.name); // 컴파일 에러: private 필드에 직접 접근 불가

		// 게터 메서드를 통해 필드에 접근 (게터 메서드는 자동으로 생성됨)
		System.out.println(person.name());
		System.out.println(person.age());
		System.out.println(person.greet());
		System.out.println(person.toString());

		Person person2 = new Person("둘리", 10);
		Person person3 = new Person("둘리", 11);

		System.out.println(person.equals(person2));
		System.out.println(person.equals(person3));
	}
}
