package org.hyperic.hq.alert.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public interface AlertRepositoryCustom {

    @Transactional(readOnly = true)
    long countByCreateTimeAndPriority(long begin, long end, int priority, boolean inEsc,
                                         boolean notFixed, Set<Integer> groupMembers, Integer alertDefId);

    @Transactional
    int deleteByAlertDefinition(AlertDefinition def);

    @Transactional
    int deleteByCreateTime(long before, int maxDeletes);
    
    @Transactional(readOnly = true)
    Page<Alert> findByCreateTimeAndPriority(long begin, long end, int priority, boolean inEsc,
                                            boolean notFixed, Set<Integer> groupMembers, Integer alertDefId,
                                            Pageable pageable);
    @Transactional(readOnly = true)
    List<Alert> findByCreateTimeAndPriority(long begin, long end, int priority, boolean inEsc,
                                            boolean notFixed, Set<Integer> groupMembers, Integer alertDefId,
                                            Sort sort);

    @Transactional(readOnly = true)
    List<Alert> findByResourceInRange(Integer res, long begin, long end, boolean nameSort,
                                      boolean asc);

    @Transactional(readOnly = true)
    Alert findLastByDefinition(AlertDefinition def, boolean fixed);

    @Transactional(readOnly = true)
    long getOldestUnfixedAlertTime();
    
    @Transactional(readOnly = true)
    Map<Integer, Map<AlertInfo, Integer>> getUnfixedAlertInfoAfter(long ctime);
    
    @Transactional(readOnly = true)
    boolean isAckable(Alert alert);
}
