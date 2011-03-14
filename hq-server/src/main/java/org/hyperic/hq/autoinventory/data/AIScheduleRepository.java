package org.hyperic.hq.autoinventory.data;

import java.util.List;

import org.hyperic.hq.autoinventory.AISchedule;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIScheduleRepository extends JpaRepository<AISchedule, Integer> {

    AISchedule findByScanName(String name);

    List<AISchedule> findByEntityTypeAndEntityId(int type, int id, Sort sort);

}
