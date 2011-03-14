package org.hyperic.hq.control.data;

import java.util.List;

import org.hyperic.hq.control.server.session.ControlSchedule;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlScheduleRepository extends JpaRepository<ControlSchedule, Integer> {

    List<ControlSchedule> findByResource(int resourceId);

    List<ControlSchedule> findByResource(int resourceId, Sort sort);
}
