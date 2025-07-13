package modackbulz.app.Application.domain.community.dao;

import modackbulz.app.Application.entity.CoComment;

import java.util.List;
import java.util.Optional;

public interface CoCommentDAO {

    // 댓글 목록 조회 (게시글 ID 기준)
    List<CoComment> findByPostId(Long coId);

    // 댓글 단건 조회 (댓글 ID 기준)
    Optional<CoComment> findById(Long cComId);

    // 댓글 등록
    CoComment save(CoComment comment);

    // 댓글 수정
    CoComment update(CoComment comment);

    // 댓글 삭제
    void delete(Long cComId);

    // (필요 시) 특정 회원 댓글 목록 조회
    List<CoComment> findByMemberId(Long memberId);
}
