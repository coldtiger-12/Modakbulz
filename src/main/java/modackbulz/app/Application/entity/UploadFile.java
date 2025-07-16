package modackbulz.app.Application.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadFile {

  // DB의 FILE_ID (PK)
  private Long fileId;

  // 사용자가 올린 실제 파일 이름 (예: my_cat.jpg)
  private String originName;

  // 서버에 저장될 때 중복을 피하기 위한 이름 (예: uuid_my_cat.jpg)
  private String saveName;

  // 서버에 파일이 저장된 물리적 경로 (예: /upload/review/)
  private String filePath;

  // 클라이언트(브라우저)에서 파일에 접근하기 위한 URL (예: /upload/review/uuid_my_cat.jpg)
  private String fileUrl;

  // 이 파일이 어떤 게시판에 속하는지 구분 (REVIEW, COMMUNITY, FAQ 등)
  private String boardType;

  // 연결된 게시글의 ID (예: 리뷰 ID)
  private Long boardId;

  // 파일이 업로드된 시간
  private Timestamp uploadAt;
}