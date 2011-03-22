package org.hyperic.hq.alert.data;

import java.util.List;
import java.util.Map;

import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertInfo;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface AlertRepositoryCustom {

    @Transactional
    int deleteAlertsByCreateTime(long before, int maxDeletes);

    @Transactional
    int deleteByAlertDefinition(AlertDefinition def);

    @Transactional(readOnly = true)
    List<Alert> findByCreateTimeAndPriority(long begin, long end, int priority, boolean inEsc,
                                            boolean notFixed, Integer groupId, Integer alertDefId,
                                            Pageable pageable);

    @Transactional(readOnly = true)
    List<Alert> findByResourceInRange(Resource res, long begin, long end, boolean nameSort,
                                      boolean asc);

    @Transactional(readOnly = true)
    Alert findLastByDefinition(AlertDefinition def, boolean fixed);

    @Transactional(readOnly = true)
    long getCountByCreateTimeAndPriority(long begin, long end, int priority, boolean inEsc,
                                         boolean notFixed, Integer groupId, Integer alertDefId);

    @Transactional(readOnly = true)
    Map<Integer, Map<AlertInfo, Integer>> getUnfixedAlertInfoAfter(long ctime);
}