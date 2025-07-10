package modackbulz.app.Application.domain.member.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.entity.Member;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberDAOImpl implements MemberDAO {

  private final NamedParameterJdbcTemplate template;

  @Override
  public Member insertMember(Member member) {
    StringBuffer sql = new StringBuffer();
    sql.append(" INSERT INTO MEMBER (");
    sql.append("   MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION ");
    sql.append(" ) VALUES (");
    sql.append("   member_member_id_seq.NEXTVAL, :gubun, :id, :pwd, :email, :tel, :nickname, :gender, :region ) ");

    SqlParameterSource param = new BeanPropertySqlParameterSource(member);
    KeyHolder keyHolder = new GeneratedKeyHolder();

    int rows = template.update(sql.toString(), param, keyHolder, new String[]{"MEMBER_ID"});
    long memberId = ((Number) keyHolder.getKeys().get("MEMBER_ID")).longValue();


    return findByMemeberId(memberId).orElseThrow(() -> new RuntimeException("회원 등록 실패"));
  }

  @Override
  public boolean isExist(String id) {
    String sql = "SELECT COUNT(*) FROM MEMBER WHERE ID = :id ";
    SqlParameterSource param = new MapSqlParameterSource().addValue("id", id);
    Integer cnt = template.queryForObject(sql, param, Integer.class);
    return cnt != null && cnt > 0;
  }

  @Override
  public Optional<Member> findByMemeberId(Long memberId) {
    if (memberId == null) {
      return Optional.empty(); // 혹은 IllegalArgumentException 던져도 됨
    }

    StringBuffer sql = new StringBuffer();
    sql.append(" SELECT ");
    sql.append(" MEMBER_ID, GUBUN, ID, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE ");
    sql.append(" FROM MEMBER ");
    sql.append(" WHERE MEMBER_ID = :memberId ");

    Map<String, Object> param = new HashMap<>();
    param.put("memberId", memberId);

    try {
      Member member = template.queryForObject(
          sql.toString(),
          param,
          BeanPropertyRowMapper.newInstance(Member.class)
      );
      return Optional.of(member);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Member> findById(String id) {
    StringBuffer sql = new StringBuffer();
    sql.append(" SELECT ");
    sql.append(" GUBUN, ID, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE ");
    sql.append(" FROM MEMBER ");
    sql.append(" WHERE ID = :id ");

    Map<String, String> param = Map.of("id", id);

    try {
      Member member = template.queryForObject(sql.toString(), param,
          BeanPropertyRowMapper.newInstance(Member.class));
      return Optional.of(member);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Member> login(String id, String pwd) {
    StringBuffer sql = new StringBuffer();
    sql.append(" SELECT ");
    sql.append(" MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE ");
    sql.append(" FROM MEMBER ");
    sql.append(" WHERE ID = :id AND PWD = :pwd AND IS_DEL = 'N' ");

    Map<String, String> param = Map.of("id", id, "pwd", pwd);

    try {
      Member member = template.queryForObject(sql.toString(), param,
          BeanPropertyRowMapper.newInstance(Member.class));
      return Optional.of(member);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean changePassword(Long memberId, String newPwd) {
    String sql = " UPDATE MEMBER SET PWD = :pwd WHERE MEMBER_ID = :memberId ";
    MapSqlParameterSource param = new MapSqlParameterSource()
        .addValue("pwd", newPwd)
        .addValue("memberId", memberId);
    int affected = template.update(sql, param);
    return affected == 1;
  }

  @Override
  public boolean deleteMember(Long memberId) {
    String sql = "UPDATE MEMBER SET IS_DEL = 'Y', DEL_DATE = SYSTIMESTAMP + INTERVAL '7' DAY WHERE MEMBER_ID = :memberId ";
    MapSqlParameterSource param = new MapSqlParameterSource().addValue("memberId", memberId);
    int affected = template.update(sql, param);
    return affected == 1;
  }

  @Override
  public boolean updateMember(Member member) {
    String sql = "UPDATE MEMBER SET " +
        "TEL = :tel, " +
        "NICKNAME = :nickname, " +
        "REGION = :region, " + // 쉼표 추가
        "EMAIL = :email " + // 이메일 업데이트 추가
        "WHERE MEMBER_ID = :memberId";

    Map<String, Object> param = new HashMap<>();
    param.put("tel", member.getTel());
    param.put("nickname", member.getNickname());
    param.put("region", member.getRegion());
    param.put("email", member.getEmail()); // 파라미터 추가
    param.put("memberId", member.getMemberId());

    int updated = template.update(sql, param);
    return updated == 1;
  }
}