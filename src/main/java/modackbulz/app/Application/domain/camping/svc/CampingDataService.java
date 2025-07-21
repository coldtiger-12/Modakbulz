package modackbulz.app.Application.domain.camping.svc;

import modackbulz.app.Application.domain.camping.dto.CampingDataDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

@Service
public class CampingDataService {

  public List<CampingDataDto> getAllCampingData() {
    List<CampingDataDto> campingList = new ArrayList<>();

    try (InputStream is = new ClassPathResource("camping_data.xlsx").getInputStream();
         Workbook workbook = new XSSFWorkbook(is)) {

      Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용

      // 헤더 행을 건너뛰고 데이터 행부터 읽기
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row != null) {
          CampingDataDto camping = new CampingDataDto();

          // 엑셀 파일의 실제 컬럼 구조에 맞게 수정
          camping.setFacltNm(getCellValueAsString(row.getCell(1))); // B열: 캠핑장명
          camping.setMapX(getCellValueAsString(row.getCell(22)));   // W열: 경도 (mapX)
          camping.setMapY(getCellValueAsString(row.getCell(23)));   // X열: 위도 (mapY)
          camping.setAddr1(getCellValueAsString(row.getCell(20)));  // U열: 주소 (addr1)
          camping.setAddr2(getCellValueAsString(row.getCell(21)));  // V열: 추가 주소 (addr2)
          camping.setTel(getCellValueAsString(row.getCell(25)));    // Z열: 전화번호 (tel)

          // 좌표가 있는 경우에만 추가
          if (camping.getMapX() != null && camping.getMapY() != null &&
              !camping.getMapX().isEmpty() && !camping.getMapY().isEmpty()) {
            campingList.add(camping);
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }

    return campingList;
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) {
      return null;
    }

    switch (cell.getCellType()) {
      case STRING:
        return cell.getStringCellValue();
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return cell.getDateCellValue().toString();
        } else {
          return String.valueOf(cell.getNumericCellValue());
        }
      case BOOLEAN:
        return String.valueOf(cell.getBooleanCellValue());
      case FORMULA:
        return cell.getCellFormula();
      default:
        return null;
    }
  }
}