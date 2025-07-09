package modackbulz.app.Application.domain.community.dao;

import modackbulz.app.Application.entity.Community;

import java.util.List;
import java.util.Optional;

public interface CommunityDAO {

  // 전체 게시글 조회
  List<Community> findAll();

  // 게시글 단건 조회
  Optional<Community> findById(Long id);

  // 게시글 등록
  Community save(Community community);

  // 게시글 수정
  Community update(Community community);

  // 게시글 삭제
  void delete(Long id);

  // 회원 ID 기준 게시글 목록 조회
  List<Community> findByMemberId(Long memberId);

  // 게시글 조회수 증가 (VIEW_C + 1)
  void increaseViewCount(Long id);

  // 최근 게시글 불러오기
  List<Community> findRecentPosts(int count);
}

