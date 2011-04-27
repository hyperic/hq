package org.hyperic.hq.galert.data;

import java.util.List;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface GalertLogRepositoryCustom {

    Page<GalertLog> findByCreateTimeAndPriority(long begin, long end, AlertSeverity severity,
                                                boolean inEscalation, boolean notFixed,
                                                Integer groupId, Integer galertDefId,
                                                Pageable pageable);

    List<GalertLog> findByCreateTimeAndPriority(long begin, long end, AlertSeverity severity,
                                                boolean inEscalation, boolean notFixed,
                                                Integer groupId, Integer galertDefId, Sort sort);

    Page<GalertLog> findByGroupAndTimestampBetween(Integer group, long begin, long end,
                                                   Pageable pageable);

    GalertLog findLastByDefinition(GalertDef def, boolean fixed);

    boolean hasEscalationState(GalertLog galertLog);

    boolean isAcknowledgeable(GalertLog galertLog);

    boolean isAcknowledged(GalertLog galertLog);
}
