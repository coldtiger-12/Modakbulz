package modackbulz.app.Application.domain.community.svc;

import modackbulz.app.Application.entity.CoComment;

import java.util.List;
import java.util.Optional;

public interface CoCommentSVC {

    // 게시글 댓글 목록 조회
    List<CoComment> getCommentsByPostId(Long coId);

    // 댓글 단건 조회
    Optional<CoComment> getCommentById(Long cComId);

    // 댓글 작성
    CoComment createComment(CoComment comment);

    // 댓글 수정
    CoComment updateComment(CoComment comment);

    // 댓글 삭제
    void deleteComment(Long cComId);

    // 회원별 댓글 목록 조회 (필요 시)
    List<CoComment> getCommentsByMemberId(Long memberId);
}