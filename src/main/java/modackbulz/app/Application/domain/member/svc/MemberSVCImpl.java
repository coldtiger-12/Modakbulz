package modackbulz.app.Application.domain.member.svc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.domain.member.dao.MemberDAO;
import modackbulz.app.Application.entity.Member;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSVCImpl implements MemberSVC {

  private final MemberDAO memberDAO;

  @Override
  public Member insertMember(Member member) {
    return memberDAO.insertMember(member);
  }

  @Override
  public boolean isExist(String id) {
    return memberDAO.isExist(id);
  }

  @Override
  public Optional<Member> findByMemeberId(Long memberId) {
    return memberDAO.findByMemeberId(memberId);
  }

  @Override
  public Optional<Member> findById(String id) {
    return memberDAO.findById(id);
  }

  @Override
  public Optional<Member> login(String id, String pwd) {
    return memberDAO.login(id, pwd);
  }

  @Override
  public boolean changePassword(Long memberId, String newPwd) {
    return memberDAO.changePassword(memberId, newPwd);
  }

  @Override
  public boolean deleteMember(Long memberId) {
    return memberDAO.deleteMember(memberId);
  }

  @Override
  public boolean updateMember(Member member) {
    return memberDAO.updateMember(member);
  }
}