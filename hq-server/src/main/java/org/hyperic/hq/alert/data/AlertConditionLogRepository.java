package org.hyperic.hq.alert.data;

import org.hyperic.hq.events.server.session.AlertConditionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertConditionLogRepository extends JpaRepository<AlertConditionLog, Integer> {

}
