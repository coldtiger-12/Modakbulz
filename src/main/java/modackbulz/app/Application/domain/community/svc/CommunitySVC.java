package modackbulz.app.Application.domain.community.svc;

import modackbulz.app.Application.entity.Community;

import java.util.List;
import java.util.Optional;

public interface CommunitySVC {

  // 전체 게시글 조회
  List<Community> getAllPosts();

  // 특정 게시글 조회
  Optional<Community> getPostById(Long id);

  // 게시글 저장
  Community createPost(Community community);

  // 게시글 수정
  Community updatePost(Community community);

  // 게시글 삭제
  void deletePost(Long id);

  // 특정 회원의 게시글만 조회
  List<Community> getPostsByMemberId(Long memberId);

  // 조회수 증가
  void increaseViewCount(Long id);

}
