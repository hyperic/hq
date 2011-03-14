package org.hyperic.hq.control.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.control.server.session.ControlFrequency;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class ControlHistoryRepositoryImpl implements ControlHistoryRepositoryCustom {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<ControlFrequency> getControlFrequencies(int numToReturn) throws SQLException {
        String sqlStr = "SELECT resource_id, action, COUNT(id) AS num " +
                        "FROM EAM_CONTROL_HISTORY " +
                        "WHERE scheduled = " +
                        DBUtil.getBooleanValue(false, jdbcTemplate.getDataSource().getConnection()) +
                        " GROUP BY resource_id, action " + "ORDER by num DESC ";

        List<ControlFrequency> frequencies = new ArrayList<ControlFrequency>();
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(sqlStr);

        for (int i = 0; i < numToReturn && i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            frequencies.add(new ControlFrequency(((Number) row.get("resource_id")).intValue(),
                (String) row.get("action"), ((Number) row.get("num")).longValue()));
        }
        return frequencies;
    }

}
