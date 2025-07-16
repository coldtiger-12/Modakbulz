package modackbulz.app.Application.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import modackbulz.app.Application.entity.UploadFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FileStore {

  @Value("${file.dir}")
  private String fileDir;

  public String getFullPath(String filename) {
    return fileDir + filename;
  }

  public List<UploadFile> storeFiles(List<MultipartFile> multipartFiles) throws IOException {
    List<UploadFile> storeFileResult = new ArrayList<>();
    if (multipartFiles != null) {
      for (MultipartFile multipartFile : multipartFiles) {
        if (!multipartFile.isEmpty()) {
          storeFileResult.add(storeFile(multipartFile));
        }
      }
    }
    return storeFileResult;
  }

  public UploadFile storeFile(MultipartFile multipartFile) throws IOException {
    if (multipartFile == null || multipartFile.isEmpty()) {
      return null;
    }

    String originalFilename = multipartFile.getOriginalFilename();
    String storeFileName = createStoreFileName(originalFilename);

    // 파일을 서버에 저장
    multipartFile.transferTo(new File(getFullPath(storeFileName)));

    return UploadFile.builder()
        .originName(originalFilename)
        .saveName(storeFileName)
        .filePath(fileDir) // DB에 저장될 경로
        .fileUrl("/upload-images/" + storeFileName)
        .build();
  }

  private String createStoreFileName(String originalFilename) {
    String ext = extractExt(originalFilename);
    String uuid = UUID.randomUUID().toString();
    return uuid + "." + ext;
  }

  private String extractExt(String originalFilename) {
    int pos = originalFilename.lastIndexOf(".");
    return originalFilename.substring(pos + 1);
  }

  /**
   * [추가] 서버에서 특정 파일들을 삭제하는 메소드
   * @param filesToDelete 삭제할 UploadFile 객체 리스트
   */
  public void deleteFiles(List<UploadFile> filesToDelete) {
    if (filesToDelete == null || filesToDelete.isEmpty()) {
      return;
    }
    for (UploadFile file : filesToDelete) {
      if (file != null && file.getSaveName() != null) {
        File storedFile = new File(getFullPath(file.getSaveName()));
        if (storedFile.exists()) {
          storedFile.delete();
        }
      }
    }
  }
}