package com.example.springai3;

public record Person(String name, int age) {
    // 기본 생성자 외에 추가 로직을 포함할 수 있는 생성자
    public Person {
        if (age < 0) {
            throw new IllegalArgumentException("나이는 음의 값이 될 수 없어요..");
        }
    }
    // 추가 메서드 정의 가능
    public String greet() {
        return "안녕? 나의 이름은 " + name + "!!.. 그리고 나의 나이는 " + age + "세야~~";
    }
}