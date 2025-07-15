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
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
    http
        // 1. 인가(Authorization) 설정
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                "/",
                "/login",
                "/member/join",
                "/member/check-nickname", // 닉네임 중복 확인 API 경로 허용
                "/member/email/**",       // 이메일 인증 API 경로 허용
                "/member/verify-email",
                "/camping/**",
                "/posts/community/**",
                "/css/**",
                "/js/**",
                "/images/**",
                "/fonts/**",
                "/api/scraps/**"
            ).permitAll()
            .requestMatchers("/admin/**").hasRole("A")
            .requestMatchers("/api/scraps/**").authenticated()
            .anyRequest().authenticated()
        )

        // 2. 커스텀 로그인 설정
        .formLogin(form -> form
            .loginPage("/login")
            .loginProcessingUrl("/login")
            .usernameParameter("id")
            .passwordParameter("pwd")
            .defaultSuccessUrl("/", true)
            .permitAll()
        )

        // 3. 로그아웃 설정
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/")
            .invalidateHttpSession(true) // 세션 무효화
            .deleteCookies("JSESSIONID") // 쿠키 삭제
            .permitAll()
        )

        // 4. 접근 거부 핸들러
        .exceptionHandling(exception -> exception
            .accessDeniedPage("/access-denied")
        );

    return http.build();
  }
}