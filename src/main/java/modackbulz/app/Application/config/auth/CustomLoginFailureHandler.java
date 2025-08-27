package modackbulz.app.Application.config.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException{

    String errorMessage;

    if (exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
      // 1. 비밀번호가 틀렸거나, 아이디가 존재하지 않을 때
      errorMessage = "login-error";           // 아이디 또는 비밀번호가 맞지 않습니다.
    } else {
      // 2. 그 외 다른 종류의 로그인 실패
      errorMessage = "unknown-error";       //  알 수 없는 오류로 로그인에 실패했습니다.
    }

    // ⭐️ 에러 메시지를 URL에 담아 로그인 페이지로 다시 보냅니다.
    String encodedErrorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
    String redirectUrl = "/login?error=true&message=" + encodedErrorMessage;
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}