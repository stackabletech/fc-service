package eu.xfsc.fc.core.dao.catalogue.impl;

import eu.xfsc.fc.core.dao.catalogue.CESTrackerDao;
import eu.xfsc.fc.core.pojo.catalogue.CESTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Component
public class CESTrackerDaoImpl implements CESTrackerDao {
    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public CESTracker getByCesId(String cesId) {
        String sql = "SELECT * FROM ces_process_tracker Where ces_id=?";
        try {
            return jdbc.queryForObject(sql, new Object[]{cesId}, new RowMapper<>() {
                @Override
                public CESTracker mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new CESTracker(rs.getString(2), rs.getString(3), rs.getString(4), rs.getLong(5));
                }
            });
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Override
    public String fetchLastIngestedEvent() {
        String sql = "SELECT ces_id FROM ces_process_tracker ORDER BY created_at DESC LIMIT 1";
        try {
            return jdbc.queryForObject(sql, new RowMapper<>() {
                @Override
                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return rs.getString(1);
                }
            });
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    @Override
    public CESTracker create(String cesId, Long status, String reason, String credential) {
        CESTracker tracker = getByCesId(cesId);
        if (Objects.nonNull(tracker)) {
            String sql = """
                    UPDATE public.ces_process_tracker
                    SET reason=?, credential=?, status=?, updated_at=?
                    WHERE ces_id=?;
                    """;
            jdbc.update(sql, new Object[]{reason, credential, status, new Date(), cesId});
        } else {
            String sql = """
                    INSERT INTO public.ces_process_tracker
                    (ces_id, reason, credential, status, created_at)
                    VALUES(?, ?, ?, ?, ?);
                    """;
            jdbc.update(sql, new Object[]{cesId, reason, credential, status, new Date()});
        }
        return getByCesId(cesId);
    }

}
