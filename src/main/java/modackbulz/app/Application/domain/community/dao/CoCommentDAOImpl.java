package modackbulz.app.Application.domain.community.dao;

import lombok.RequiredArgsConstructor;
import modackbulz.app.Application.entity.CoComment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CoCommentDAOImpl implements CoCommentDAO {

    private final NamedParameterJdbcTemplate template;

    @Override
    public List<CoComment> findByPostId(Long coId) {
        String sql = "SELECT * FROM CO_COMMENT WHERE CO_ID = :coId ORDER BY CREATED_AT ASC";
        return template.query(sql, Map.of("coId", coId), (rs, rowNum) -> {
            CoComment c = new CoComment();
            c.setCComId(rs.getLong("C_COM_ID"));
            c.setCoId(rs.getLong("CO_ID"));
            c.setMemberId(rs.getLong("MEMBER_ID"));
            c.setWriter(rs.getString("WRITER"));
            c.setContent(rs.getString("CONTENT"));

            Timestamp created = rs.getTimestamp("CREATED_AT");
            Timestamp updated = rs.getTimestamp("UPDATED_AT");

            if (created != null) c.setCreatedAt(created.toLocalDateTime());
            if (updated != null) c.setUpdatedAt(updated.toLocalDateTime());

            c.setPrcComId(rs.getObject("PRC_COM_ID", Long.class));
            return c;
        });
    }


    @Override
    public Optional<CoComment> findById(Long cComId) {
        String sql = "SELECT * FROM CO_COMMENT WHERE C_COM_ID = :cComId";
        try {
            CoComment comment = template.queryForObject(sql, Map.of("cComId", cComId), BeanPropertyRowMapper.newInstance(CoComment.class));
            return Optional.of(comment);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public CoComment save(CoComment comment) {
        String sql = "INSERT INTO CO_COMMENT (C_COM_ID, CO_ID, MEMBER_ID, WRITER, CONTENT, CREATED_AT, PRC_COM_ID) " +
                "VALUES (co_comment_seq.NEXTVAL, :coId, :memberId, :writer, :content, SYSTIMESTAMP, :prcComId)";
        SqlParameterSource param = new BeanPropertySqlParameterSource(comment);
        template.update(sql, param);
        return comment;
    }

    @Override
    public CoComment update(CoComment comment) {
        String sql = "UPDATE CO_COMMENT SET CONTENT = :content, UPDATED_AT = SYSTIMESTAMP WHERE C_COM_ID = :cComId";
        SqlParameterSource param = new BeanPropertySqlParameterSource(comment);
        template.update(sql, param);
        return comment;
    }

    @Override
    public void delete(Long cComId) {
        String sql = "DELETE FROM CO_COMMENT WHERE C_COM_ID = :cComId";
        template.update(sql, Map.of("cComId", cComId));
    }

    @Override
    public List<CoComment> findByMemberId(Long memberId) {
        String sql = "SELECT * FROM CO_COMMENT WHERE MEMBER_ID = :memberId ORDER BY CREATED_AT DESC";
        return template.query(sql, Map.of("memberId", memberId), BeanPropertyRowMapper.newInstance(CoComment.class));
    }
}
