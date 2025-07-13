package modackbulz.app.Application.domain.community.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.community.dao.CoCommentDAO;
import modackbulz.app.Application.entity.CoComment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoCommentSVCImpl implements CoCommentSVC {

    private final CoCommentDAO coCommentDao;

    // 자식 댓글을 재귀적으로 찾는 내부 메소드
    private void buildRepliesRecursively(CoComment parent, Map<Long, List<CoComment>> childMap) {
        // 현재 부모 댓글의 ID를 키로 사용하여 자식 목록을 찾음
        List<CoComment> children = childMap.get(parent.getCComId());

        // 자식 목록이 존재하면
        if (children != null) {
            // 자식 목록에서 null을 한번 더 필터링 (안전장치)
            children = children.stream()
                    .filter(Objects::nonNull)
                    .toList();

            // 부모 객체에 자식 목록을 설정
            parent.setReplies(children);

            // 각 자식 댓글에 대해 이 과정을 재귀적으로 반복
            for (CoComment child : children) {
                buildRepliesRecursively(child, childMap);
            }
        }
    }

    @Override
    public List<CoComment> getCommentsByPostId(Long coId) {
        // 1. 게시글에 달린 모든 댓글을 DB에서 가져옴 (평평한 리스트)
        List<CoComment> flatList = coCommentDao.findByPostId(coId);

        // 2. (안전장치) DB에서 가져온 리스트에 혹시 모를 null이 있다면 제거
        flatList = flatList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. 최상위 부모 댓글들만 필터링 (prcComId가 null인 댓글)
        List<CoComment> parents = flatList.stream()
                .filter(c -> c.getPrcComId() == null)
                .toList();

        // 4. 자식 댓글들을 부모 ID를 기준으로 그룹화하여 Map에 저장
        // (예: { 100번댓글: [101번, 102번], 103번댓글: [104번] })
        Map<Long, List<CoComment>> childMap = flatList.stream()
                .filter(c -> c.getPrcComId() != null)
                .collect(Collectors.groupingBy(CoComment::getPrcComId));

        // 5. 최상위 부모 댓글들을 순회하며 재귀적으로 자식들을 연결
        for (CoComment parent : parents) {
            buildRepliesRecursively(parent, childMap);
        }

        // 6. 계층 구조가 완성된 최상위 부모 댓글 목록을 반환
        return parents;
    }



    @Override
    public Optional<CoComment> getCommentById(Long cComId) {
        return coCommentDao.findById(cComId);
    }

    @Override
    public CoComment createComment(CoComment comment) {
        return coCommentDao.save(comment);
    }

    @Override
    public CoComment updateComment(CoComment comment) {
        return coCommentDao.update(comment);
    }

    @Override
    public void deleteComment(Long cComId) {
        coCommentDao.delete(cComId);
    }

    @Override
    public List<CoComment> getCommentsByMemberId(Long memberId) {
        return coCommentDao.findByMemberId(memberId);
    }
}