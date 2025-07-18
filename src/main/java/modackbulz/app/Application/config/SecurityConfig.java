package modackbulz.app.Application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. 인가(Authorization) 설정
        .authorizeHttpRequests(authorize -> authorize
            // 닉네임 중복 확인 API는 POST 방식이므로 명시적으로 허용
            .requestMatchers(HttpMethod.POST, "/member/check-nickname").permitAll()
            .requestMatchers(
                "/", "/login", "/logout",
                "/member/**", // 회원가입 관련 모든 하위 경로 허용
                "/camping/**",
                "/posts/community/**",
                "/api/**",
                "/css/**", "/js/**", "/images/**", "/fonts/**", "/upload-images/**"
            ).permitAll()
            .requestMatchers("/admin/**").hasRole("A")
            .anyRequest().authenticated()
        )

        // 2. 로그인 설정
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
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
            .permitAll()
        )

        // 4. 접근 거부 핸들러
        .exceptionHandling(exception -> exception
            .accessDeniedPage("/access-denied")
        )

        // 5. CSRF 보호 설정 (API 및 회원가입 관련 경로는 비활성화)
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**", "/member/**")
        );

    return http.build();
  }
}