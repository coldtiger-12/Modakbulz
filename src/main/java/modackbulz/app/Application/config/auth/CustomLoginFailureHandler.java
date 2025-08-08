package modackbulz.app.Application.config.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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
      errorMessage = "login-error";
    } else if (exception instanceof DisabledException) {
      // 2. 계정이 비활성화된 경우 (우리의 '탈퇴 요청자'가 여기에 해당)
      // 이 경우에는 에러 메시지 없이, 탈퇴 취소 페이지로 바로 보냅니다.
      response.sendRedirect(request.getContextPath() + "/member/confirm-cancel-withdrawal");
      return; // 여기서 처리를 완전히 끝냅니다.
    } else {
      // 3. 그 외 다른 종류의 로그인 실패
      errorMessage = "알 수 없는 이유로 로그인에 실패하였습니다. 관리자에게 문의하세요.";
    }

    // ⭐️ 에러 메시지를 URL에 담아 로그인 페이지로 다시 보냅니다.
    errorMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
    response.sendRedirect(request.getContextPath() + "/login?error=true&message=" + errorMessage);
  }
}
