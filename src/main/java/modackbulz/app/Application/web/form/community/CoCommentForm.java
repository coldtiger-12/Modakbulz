package modackbulz.app.Application.web.form.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CoCommentForm {
    private Long cComId;    // 댓글 ID (수정 시 필요)
    private Long coId;      // 게시글 ID (댓글이 속한 게시글)
    private Long prcComId;  // 부모 댓글 ID (대댓글 기능 시 사용, 없으면 null)
    private String writer;

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 1000, message = "댓글은 최대 1000자까지 작성할 수 있습니다.")
    private String content;
}