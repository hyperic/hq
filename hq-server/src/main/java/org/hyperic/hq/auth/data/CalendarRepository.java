package org.hyperic.hq.auth.data;

import org.hyperic.hq.auth.domain.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarRepository extends JpaRepository<Calendar, Integer>{

}
