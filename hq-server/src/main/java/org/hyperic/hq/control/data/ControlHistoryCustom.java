package org.hyperic.hq.control.data;

import java.sql.SQLException;
import java.util.List;

import org.hyperic.hq.control.server.session.ControlFrequency;

public interface ControlHistoryCustom {

    List<ControlFrequency> getControlFrequencies(int numToReturn) throws SQLException;
}
