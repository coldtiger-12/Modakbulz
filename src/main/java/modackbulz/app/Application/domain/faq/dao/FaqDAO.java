package modackbulz.app.Application.domain.faq.dao;

import modackbulz.app.Application.entity.Faq;

import java.util.List;
import java.util.Optional;

public interface FaqDAO {
  Faq insert(Faq faq);
  List<Faq> findAll();
  List<Faq> findByMemberId(Long memberId);
  Optional<Faq> findById(Long faqId);
  int delete(Long faqId);
}
