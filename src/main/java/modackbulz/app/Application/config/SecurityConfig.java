package modackbulz.app.Application.config;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // 1. 인가(Authorization) 설정
        .authorizeHttpRequests(auth -> auth
            // POST: 닉네임 중복 체크 허용
            .requestMatchers(HttpMethod.POST, "/member/check-nickname").permitAll()

            // 🎯 인증이 필요한 API
            .requestMatchers("/api/scraps/**").authenticated()

            // 나머지 공개 API
            .requestMatchers(
                "/", "/login", "/logout",
                "/member/**",
                "/camping/**",
                "/posts/community/**",
                "/api/**",  // ❗ 여기서 /api/**는 제외하거나 scrap보다 아래로 내려야 함
                "/css/**", "/js/**", "/images/**", "/fonts/**", "/upload-images/**"
            ).permitAll()

            // 관리자 페이지
            .requestMatchers("/admin/**").hasRole("A")

            // 나머지는 인증 필요
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

        // 5. CSRF 보호 설정 (API 및 회원가입 관련 경로는 비활성화, admin 경로도 비활성화 추가)
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**", "/member/**", "/admin/**")
        );

    return http.build();
  }
}