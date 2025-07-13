package modackbulz.app.Application.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CoComment {

    private Long cComId;       // 댓글 ID (PK)
    private Long coId;         // 게시글 ID (FK, COMMUNITY.CO_ID)
    private Long memberId;     // 작성자 회원 ID (FK, MEMBER.MEMBER_ID)
    private String writer;     // 작성자 닉네임
    private String content;    // 댓글 내용
    private LocalDateTime createdAt;  // 작성일
    private LocalDateTime updatedAt;  // 수정일
    private Long prcComId;     // 부모 댓글 ID (대댓글 기능용, null 가능, 자기 참조)

    //대댓글용 자식 댓글 담을 필드
    private List<CoComment> replies = new ArrayList<>();
    public List<CoComment> getReplies() {
        return replies;
    }
    public void setReplies(List<CoComment> replies) {
        this.replies = replies;
    }


}
