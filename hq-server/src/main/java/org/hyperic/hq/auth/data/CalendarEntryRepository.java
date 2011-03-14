package org.hyperic.hq.auth.data;

import org.hyperic.hq.auth.domain.CalendarEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEntryRepository extends JpaRepository<CalendarEntry, Integer> {

}
