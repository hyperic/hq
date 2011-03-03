package org.hyperic.hq.control.data;

import java.util.List;

import org.hyperic.hq.control.server.session.ControlHistory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControlHistoryRepository extends JpaRepository<ControlHistory, Integer>,
    ControlHistoryCustom {

    List<ControlHistory> findByGroupIdAndBatchId(int groupId, int batchId, Sort sort);

    List<ControlHistory> findByResource(int resourceId);

    List<ControlHistory> findByResource(int resourceId, Sort sort);

    List<ControlHistory> findByStartTimeGreaterThanOrderByStartTimeDesc(long startTime);
}
