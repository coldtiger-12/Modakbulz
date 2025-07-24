package modackbulz.app.Application.domain.member.dao;

import lombok.extern.slf4j.Slf4j;
import modackbulz.app.Application.entity.Member;
import modackbulz.app.Application.entity.MemberStatus;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
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

  // 모든 회원 정보 조회 (아이디 / 비밀번호 찾기에 이용 됨)
  @Override
  public List<Member> findAll(){
    String sql = " SELECT MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE " +
        " FROM MEMBER WHERE IS_DEL = 'ACTIVE' " ;

    // ❗️ RowMapper를 이 메소드 안에서 직접 만듭니다.
    RowMapper<Member> rowMapper = (rs, rowNum) -> {
      Member member = new Member();
      member.setMemberId(rs.getLong("MEMBER_ID"));
      member.setGubun(rs.getString("GUBUN"));
      member.setId(rs.getString("ID"));
      member.setNickname(rs.getString("NICKNAME"));
      member.setGender(rs.getString("GENDER"));
      member.setRegion(rs.getString("REGION"));
      // 이제 이 시점에는 encryptService가 존재하므로 정상적으로 사용할 수 있습니다.
      member.setEmail(encryptService.decrypt(rs.getString("EMAIL")));
      member.setTel(encryptService.decrypt(rs.getString("TEL")));

      // DB의 'IS_DEL' 컬럼 값을 가져와서 'Y'이면 PENDING_DELETION, 아니면 ACTIVE로 설정
      String isDelStatus = rs.getString("IS_DEL");
      member.setStatus("PENDING_DELETION".equals(isDelStatus) ? MemberStatus.PENDING_DELETION : MemberStatus.ACTIVE);

      if (rs.getTimestamp("DEL_DATE") != null) {
        member.setDeletionRequestedAt(rs.getTimestamp("DEL_DATE").toLocalDateTime());
      }
      return member;
    };

    return template.query(sql, rowMapper);


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
      // 이제 이 시점에는 encryptService가 존재하므로 정상적으로 사용할 수 있습니다.
      member.setEmail(encryptService.decrypt(rs.getString("EMAIL")));
      member.setTel(encryptService.decrypt(rs.getString("TEL")));

      // DB의 'IS_DEL' 컬럼 값을 가져와서 'Y'이면 PENDING_DELETION, 아니면 ACTIVE로 설정
      String isDelStatus = rs.getString("IS_DEL");
      member.setStatus("Y".equals(isDelStatus) ? MemberStatus.PENDING_DELETION : MemberStatus.ACTIVE);

      if (rs.getTimestamp("DEL_DATE") != null) {
        member.setDeletionRequestedAt(rs.getTimestamp("DEL_DATE").toLocalDateTime());
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

  // 로그인 로직 수정(07-23)
  @Override
  public Optional<Member> findById(String id) {
    // ⭐️ 1단계: 어떤 ID로 조회를 시도하는지 로그를 남깁니다.
    log.info(">>>> findById 호출: ID = '{}'", id);
    // ⭐️ 중요: 탈퇴 요청중인 회원도 로그인 자체는 해야하므로 IS_DEL 조건은 제거합니다.
    String sql = " SELECT MEMBER_ID, GUBUN, ID, PWD, EMAIL, TEL, NICKNAME, GENDER, REGION, IS_DEL, DEL_DATE " +
        " FROM MEMBER WHERE ID = :id ";

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
      member.setEmail(encryptService.decrypt(rs.getString("EMAIL")));
      member.setTel(encryptService.decrypt(rs.getString("TEL")));

      // DB의 'IS_DEL' 컬럼 값을 가져와서 'Y'이면 PENDING_DELETION, 아니면 ACTIVE로 설정
      String isDelStatus = rs.getString("IS_DEL");
      member.setStatus("PENDING_DELETION".equals(isDelStatus) ? MemberStatus.PENDING_DELETION : MemberStatus.ACTIVE);

      if (rs.getTimestamp("DEL_DATE") != null) {
        member.setDeletionRequestedAt(rs.getTimestamp("DEL_DATE").toLocalDateTime());
      }
      // 'PWD' 컬럼이 ResultSet에 존재할 경우에만 비밀번호 설정
      if (hasColumn(rs,"PWD")){
        member.setPwd(rs.getString("PWD"));
      }
      // ⭐️ 2단계: RowMapper가 성공적으로 실행되었는지 로그를 남깁니다.
      log.info(">>>> RowMapper 실행 성공: 사용자 '{}' 객체 생성 완료", member.getId());
      return member;
    };

    try {
      // 통합된 RowMapper를 사용하여 조회 및 복호화를 한 번에 처리
      Member member = template.queryForObject(sql, Map.of("id", id), rowMapper);
      // ⭐️ 3단계: 사용자를 성공적으로 찾았는지 로그를 남깁니다.
      log.info(">>>> queryForObject 성공: 사용자 '{}'를 DB에서 찾았습니다.", id);
      return Optional.ofNullable(member);
    } catch (EmptyResultDataAccessException e) {
      // ⭐️ 4단계: 사용자를 찾지 못했을 때 로그를 남깁니다.
      log.error(">>>> queryForObject 실패: ID '{}'에 해당하는 사용자를 DB에서 찾을 수 없습니다. (EmptyResultDataAccessException 발생)", id);
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

  /**
   * 7일뒤 탈퇴 요청
   * @param memberId
   * @return 요청 성공 여부
   */
  @Override
  public boolean requestDeletion(Long memberId){
//    LocalDateTime deletionDate = LocalDateTime.now().plusDays(7);
    LocalDateTime deletionDate = LocalDateTime.now().plusMinutes(5);

    String sql = "UPDATE MEMBER SET IS_DEL = :status, DEL_DATE = :delDate WHERE MEMBER_ID = :memberId ";
    MapSqlParameterSource param = new MapSqlParameterSource()
        .addValue("status", MemberStatus.PENDING_DELETION.name())
        .addValue("delDate",deletionDate) // 계산된 날짜 전달
        .addValue("memberId",memberId);
    int affected = template.update(sql, param);
    return affected == 1;
  }

  /**
   * 탈퇴 취소 메서드
   * @param memberId
   * @return 탈퇴 취소 성공 여부
   */
  @Override
  public boolean cancelDeletion(Long memberId){
    String sql = "UPDATE MEMBER SET IS_DEL = :status, DEL_DATE = NULL WHERE MEMBER_ID = :memberId AND IS_DEL = :currentStatus ";
    MapSqlParameterSource param = new MapSqlParameterSource()
        .addValue("status", MemberStatus.ACTIVE.name())
        .addValue("memberId", memberId)
        .addValue("currentStatus", MemberStatus.PENDING_DELETION.name());
    int affected = template.update(sql, param);
    return affected == 1;
  }

  /**
   * 스케쥴러가 7일 지난 탈퇴 요청 멤버를 찾는 메소드
   * @param criteriaDate
   * @return 탈퇴 요청 7일 지난 멤버 정보
   */

  @Override
  public List<Member> findMembersForHard(LocalDateTime criteriaDate){
    String sql = "SELECT MEMBER_ID FROM MEMBER WHERE IS_DEL = :status AND DEL_DATE <= :criteriaDate";
    SqlParameterSource param = new MapSqlParameterSource()
        .addValue("status", MemberStatus.PENDING_DELETION.name())
        .addValue("criteriaDate",criteriaDate);
    return template.query(sql, param, (rs,rowNum) -> {
      Member member = new Member();
      member.setMemberId(rs.getLong("MEMBER_ID"));
      return member;
    });
  }

  /**
   * 실제 db에서 완전 제거
   * @param memberIds
   */
  @Override
  public void hardDeleteMembers(List<Long> memberIds){
    if (memberIds == null || memberIds.isEmpty()){
      return;
    }
    String sql = "DELETE FROM MEMBER WHERE MEMBER_ID IN (:memberIds)";
    SqlParameterSource param = new MapSqlParameterSource("memberIds", memberIds);
    template.update(sql, param);

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