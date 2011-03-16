package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionStrategyTypeInfoRepository extends
    JpaRepository<ExecutionStrategyTypeInfo, Integer> {

    ExecutionStrategyTypeInfo findByType(Class<? extends ExecutionStrategyType> type);
}
