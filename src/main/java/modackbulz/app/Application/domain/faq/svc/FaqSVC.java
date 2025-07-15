package modackbulz.app.Application.domain.faq.svc;

import modackbulz.app.Application.entity.Faq;

import java.util.List;
import java.util.Optional;

public interface FaqSVC {
  Faq write(Faq faq); // 작성
  List<Faq> findAll(); // 관리자용 전체 목록
  List<Faq> findByMemberId(Long memberId); // 본인 문의 목록
  Optional<Faq> findById(Long faqId); // 상세보기 (선택)
  boolean delete(Long faqId); // 관리자 삭제
}
