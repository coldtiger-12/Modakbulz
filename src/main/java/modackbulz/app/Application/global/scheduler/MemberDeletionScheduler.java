package modackbulz.app.Application.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.dao.MemberDAO;
import modackbulz.app.Application.entity.Member;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberDeletionScheduler {

  private final MemberDAO memberDAO;

  // 매일 새벽 4시에 실행
  @Scheduled(cron = "0 0 4 * * *")
  @Transactional
  public void hardDeleteMembers(){
    log.info("탈퇴 신청 후 7일이 지난 회원 정보 삭제 작업을 시작합니다.");

    // 현재 시간을 기준으로 탈퇴 날짜에 도달한 멤버 정보를 찾음
    LocalDateTime now = LocalDateTime.now();
    List<Member> membersToDelete = memberDAO.findMembersForHard(now);

    if (membersToDelete.isEmpty()){
      log.info("삭제할 회원이 없습니다.");
      return;
    }

    List<Long> memberIds = membersToDelete.stream()
        .map(Member::getMemberId)
        .collect(Collectors.toList());

    // 여기서 관련 데이터(게시글, 댓글 등) 먼저 처리하는 로직 추가 가능(추후 추가 예정)

    memberDAO.hardDeleteMembers(memberIds);
    log.info("{}명의 회원을 데이터베이스에서 완전히 삭제했습니다",memberIds.size());

  }
}
