package org.hyperic.hq.measurement.data;

import org.hyperic.hq.measurement.server.session.ScheduleRevNum;
import org.hyperic.hq.measurement.server.session.SrnId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRevNumRepository extends JpaRepository<ScheduleRevNum, SrnId> {

}
