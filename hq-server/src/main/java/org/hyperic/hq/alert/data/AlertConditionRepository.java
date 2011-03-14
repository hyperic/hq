package org.hyperic.hq.alert.data;

import org.hyperic.hq.events.server.session.AlertCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertConditionRepository extends JpaRepository<AlertCondition, Integer> {

}
