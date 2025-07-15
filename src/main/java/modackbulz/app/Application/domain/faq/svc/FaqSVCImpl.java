package modackbulz.app.Application.domain.faq.svc;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.domain.faq.dao.FaqDAO;
import modackbulz.app.Application.entity.Faq;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FaqSVCImpl implements FaqSVC{

  private final FaqDAO faqDAO;

  @Override
  public Faq write(Faq faq) {
    return faqDAO.insert(faq);
  }

  @Override
  public List<Faq> findAll() {
    return faqDAO.findAll();
  }

  @Override
  public List<Faq> findByMemberId(Long memberId) {
    return faqDAO.findByMemberId(memberId);
  }

  @Override
  public Optional<Faq> findById(Long faqId) {
    return faqDAO.findById(faqId);
  }

  @Override
  public boolean delete(Long faqId) {
    return faqDAO.delete(faqId) > 0;
  }
}
