package org.hyperic.hq.control.data;

import java.sql.SQLException;
import java.util.List;

import org.hyperic.hq.control.server.session.ControlFrequency;
import org.springframework.transaction.annotation.Transactional;

public interface ControlHistoryRepositoryCustom {

    @Transactional(readOnly = true)
    List<ControlFrequency> getControlFrequencies(int numToReturn) throws SQLException;
}
