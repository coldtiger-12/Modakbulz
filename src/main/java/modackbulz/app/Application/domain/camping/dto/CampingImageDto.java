package modackbulz.app.Application.domain.camping.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampingImageDto {
    private Response response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Body body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Slf4j
    public static class Body {
        // [수정] API의 비정상 응답(빈 문자열)을 받기 위해 타입을 Object로 변경
        private Object items;
        private int totalCount;

        // [수정] items 필드를 안전하게 Items 객체로 변환하는 커스텀 Getter
        public Items getItems() {
            if (this.items instanceof Map) {
                // 정상적인 경우 (JSON 객체일 때), ObjectMapper를 사용해 Items 객체로 변환
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.convertValue(this.items, Items.class);
                } catch (Exception e) {
                    log.error("CampingImageDto Items 변환 실패", e);
                    return createEmptyItems();
                }
            }
            // 비정상적인 경우 (빈 문자열 "" 등), 비어있는 Items 객체를 생성하여 반환
            return createEmptyItems();
        }

        private Items createEmptyItems() {
            Items emptyItems = new Items();
            emptyItems.setItem(Collections.emptyList());
            return emptyItems;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private List<Item> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String contentId;
        private String serialnum;
        private String imageUrl;
        private String createdtime;
        private String modifiedtime;
        private String imageName;
    }
}