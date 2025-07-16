package modackbulz.app.Application.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Value("${file.dir}")
  private String fileDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 1. 새로 추가한 업로드 파일 경로 (외부 폴더)
    // /images/** URL로 요청이 오면 file:///C:/Files/ 경로에서 파일을 찾아 제공
    registry.addResourceHandler("/upload-images/**")
        .addResourceLocations("file:///" + fileDir);

    // 2. 기존에 사용하던 정적 리소스 경로 (프로젝트 내부)
    // /** URL로 요청이 오면 src/main/resources/static/ 경로에서 파일을 찾아 제공
    registry.addResourceHandler("/**")
        .addResourceLocations("classpath:/static/");
  }
}