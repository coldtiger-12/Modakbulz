package modackbulz.app.Application.config.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modackbulz.app.Application.entity.MemberStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException, SecurityException, ServletException {

    // 로그인한 사용자의 정보를 가져옴
    CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

    // 사용자의 상태를 확인
    if (userDetails.getMember().getStatus() == MemberStatus.PENDING_DELETION){
      // 만약 '탈퇴 요청 상태'라면, 탈퇴 취소 확인 페이지로
      getRedirectStrategy().sendRedirect(request, response, "/login?error");
    } else {
      // 그 외 '활성 상태'라면, 기본 설정된 성공 URL(메인 페이지)로
      super.onAuthenticationSuccess(request, response, authentication);
    }
  }
}
