package modackbulz.app.Application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder(){
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity security) throws Exception{
    security
        // 1. 인가(Authorization) 설정
        .authorizeHttpRequests(authorize -> authorize
            // 다음 경로들은 인증 없이 접근 허용
            .requestMatchers(
                "/",         // 홈페이지
                "/login",            // 로그인 페이지
                "/member/join",      // 회원가입 페이지
                "/camping/**",       // 캠핑장 목록, 검색, 상세 정보
                "/posts/community/**", // 커뮤니티 게시글 목록, 상세 정보
                "/css/**",           // 스타일시트
                "/js/**",            // 자바스크립트
                "/images/**",        // 이미지
                "/fonts/**"          // 폰트
            ).permitAll()
            // "/admin/**" 경로는 'A' 역할을 가진 사용자만 접근 가능
            .requestMatchers("/admin/**").hasRole("A")
            // 그 외 모든 요청은 인증된 사용자만 접근 가능
            .anyRequest().authenticated()
        )

        // 2. 커스텀 로그인 설정
        .formLogin(form -> form
            .loginPage("/login") // 커스텀 로그인 페이지 경로 (LoginController의 GET 메서드)
            .loginProcessingUrl("/login") // 로그인 처리 경로 (LoginController의 POST 메서드)
            .usernameParameter("id")  // 아이디는 'id'라는 이름으로 받을 것
            .passwordParameter("pwd") // 비밀번호는 'pwd'라는 이름으로 받을 것
            .defaultSuccessUrl("/", true) // 로그인 성공 시 이동할 경로
            .permitAll() // 로그인 페이지는 모두 접근 가능
        )

        .logout(logout -> logout
            .logoutUrl("/logout") // 로그아웃 처리 경로
            .logoutSuccessUrl("/") // 로그아웃 성공 시 이동할 경로
        );

    return security.build();

  }
}
