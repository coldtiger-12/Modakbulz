package modackbulz.app.Application.domain.faq.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.entity.Faq;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FaqDAOImpl implements FaqDAO{
  private  final NamedParameterJdbcTemplate template;

  public Faq insert(Faq faq) {
    String sql = """
        INSERT INTO FAQ (
          FAQ_ID, MEMBER_ID, WRITER, CONTENT, CREATED_AT
        ) VALUES (
          camp_faq_id_seq.NEXTVAL, :memberId, :writer, :content, SYSTIMESTAMP
        )
        """;
    Map<String, Object> param = new HashMap<>();
    param.put("memberId", faq.getMemberId());
    param.put("writer", faq.getWriter());
    param.put("content", faq.getContent());

    template.update(sql, param);
    return faq;
  }

  @Override
  public List<Faq> findAll() {
    String sql = "SELECT * FROM FAQ ORDER BY CREATED_AT DESC";
    return template.query(sql, new BeanPropertyRowMapper<>(Faq.class));
  }

  @Override
  public List<Faq> findByMemberId(Long memberId) {
    String sql = "SELECT * FROM FAQ WHERE MEMBER_ID = :memberId ORDER BY CREATED_AT DESC";
    return template.query(sql, Map.of("memberId",memberId), new BeanPropertyRowMapper<>(Faq.class));
  }

  @Override
  public Optional<Faq> findById(Long faqId){
    String sql = "SELECT * FROM FAQ WHERE FAQ_ID = :faqId";
    try {
      Faq faq = template.queryForObject(sql, Map.of("faqId",faqId), new BeanPropertyRowMapper<>(Faq.class));
      return Optional.ofNullable(faq);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public int delete(Long faqId) {
    String sql = "DELETE FROM FAQ WHERE FAQ_ID = :faqId";
    return template.update(sql, Map.of("faqId",faqId));
  }
}
