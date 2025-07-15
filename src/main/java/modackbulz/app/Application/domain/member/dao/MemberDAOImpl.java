package modackbulz.app.Application.domain.member.dao;

import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.global.service.EncryptService;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class MemberDAOImpl implements MemberDAO {

  private final NamedParameterJdbcTemplate template;
  private final EncryptService encryptService;

  public MemberDAOImpl(NamedParameterJdbcTemplate template, EncryptService encryptService) {
    this.template = template;
    this.encryptService = encryptService;
  }

  @Override
  public Member insertMember(Member member) {
    StringBuffer sql = new StringBuffer();
    sql.append(" INSERT INTO MEMBER (");
    sql.append("   MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION ");
    sql.append(" ) VALUES (");
    sql.append("   member_member_id_seq.NEXTVAL, :gubun, :id, :pwd, :email, :tel, :nickname, :gender, :region ) ");

    // 암호화 로직을 위한 코드 변경 - 2025.07.14
    SqlParameterSource param = new BeanPropertySqlParameterSource(encryptMember(member));
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
  public boolean isExistNickname(String nickname) {
    String sql = "SELECT COUNT(*) FROM MEMBER WHERE NICKNAME = :nickname ";
    SqlParameterSource param = new MapSqlParameterSource().addValue("nickname", nickname);
    Integer cnt = template.queryForObject(sql, param, Integer.class);
    return cnt != null && cnt > 0;
  }

  @Override
  public Optional<Member> findByMemeberId(Long memberId) {
    if (memberId == null) {
      return Optional.empty();
    }

    String sql = " SELECT MEMBER_ID, GUBUN, ID, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE " +
        " FROM MEMBER WHERE MEMBER_ID = :memberId " ;

    // ❗️ RowMapper를 이 메소드 안에서 직접 만듭니다.
    RowMapper<Member> rowMapper = (rs, rowNum) -> {
      Member member = new Member();
      member.setMemberId(rs.getLong("MEMBER_ID"));
      member.setGubun(rs.getString("GUBUN"));
      member.setId(rs.getString("ID"));
      member.setNickname(rs.getString("NICKNAME"));
      member.setGender(rs.getString("GENDER"));
      member.setRegion(rs.getString("REGION"));
      member.setIsDel(rs.getString("IS_DEL"));
      // 이제 이 시점에는 encryptService가 존재하므로 정상적으로 사용할 수 있습니다.
      member.setEmail(encryptService.decrypt(rs.getString("EMAIL")));
      member.setTel(encryptService.decrypt(rs.getString("TEL")));
      if (rs.getTimestamp("DEL_DATE") != null) {
        member.setDelDate(rs.getTimestamp("DEL_DATE").toLocalDateTime());
      }
      return member;
    };


    try {
      // 통합된 RowMapper를 사용하여 조회 및 복호화를 한 번에 처리
      Member member = template.queryForObject(sql, Map.of("memberId",memberId), rowMapper);
      return Optional.ofNullable(member);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Member> findById(String id) {
    String sql = " SELECT MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE " +
        " FROM MEMBER WHERE ID = :id AND IS_DEL = 'N' ";

    // ❗️ RowMapper를 이 메소드 안에서도 똑같이 만듭니다.
    RowMapper<Member> rowMapper = (rs, rowNum) -> {
      Member member = new Member();
      member.setMemberId(rs.getLong("MEMBER_ID"));
      member.setGubun(rs.getString("GUBUN"));
      member.setId(rs.getString("ID"));
      member.setPwd(rs.getString("PWD")); // findById 에는 PWD가 포함됩니다.
      member.setNickname(rs.getString("NICKNAME"));
      member.setGender(rs.getString("GENDER"));
      member.setRegion(rs.getString("REGION"));
      member.setIsDel(rs.getString("IS_DEL"));
      member.setEmail(encryptService.decrypt(rs.getString("EMAIL")));
      member.setTel(encryptService.decrypt(rs.getString("TEL")));
      if (rs.getTimestamp("DEL_DATE") != null) {
        member.setDelDate(rs.getTimestamp("DEL_DATE").toLocalDateTime());
      }
      // 'PWD' 컬럼이 ResultSet에 존재할 경우에만 비밀번호 설정
      if (hasColumn(rs,"PWD")){
        member.setPwd(rs.getString("PWD"));
      }
      return member;
    };

    try {
      // 통합된 RowMapper를 사용하여 조회 및 복호화를 한 번에 처리
      Member member = template.queryForObject(sql, Map.of("id", id), rowMapper);
      return Optional.ofNullable(member);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean updateMember(Member member) {
    String sql = "UPDATE MEMBER SET " +
        "TEL = :tel, " +
        "NICKNAME = :nickname, " +
        "REGION = :region, " +
        "EMAIL = :email " +
        "WHERE MEMBER_ID = :memberId";

    //DB 업데이트 전 파라미터에서 개인정보를 암호화
    Map<String, Object> param = new HashMap<>();
    param.put("tel", encryptService.encrypt(member.getTel()));
    param.put("email", encryptService.encrypt(member.getEmail()));
    param.put("nickname", member.getNickname());
    param.put("region", member.getRegion());
    param.put("memberId", member.getMemberId());

    int updated = template.update(sql, param);
    return updated == 1;
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


  /**
   * Member 객체의 개인정보 필드를 암호화하는 헬퍼 메서드
   * @param member
   * @return 암호화된 정보(이메일, 전화번호)
   */

  private Member encryptMember(Member member){
    Member encryptedMember = new Member();
    BeanUtils.copyProperties(member, encryptedMember);
    encryptedMember.setEmail(encryptService.encrypt(member.getEmail()));
    encryptedMember.setTel(encryptService.encrypt(member.getTel()));
    return encryptedMember;
  }


  /**
   * ResultSet에 특정 컬럼이 존재하는지 확인하는 헬퍼 메서드
   * @param rs
   * @param columnName
   * @return 컬럼 존재 여부
   */

  private boolean hasColumn(ResultSet rs, String columnName){
    try{
      rs.findColumn(columnName);
      return true;
    }catch (SQLException e){
      //컬럼이 존재하지 않으면 SQLException 발생
      return false;
    }
  }
}