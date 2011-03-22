package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionStrategyInfoRepository extends
    JpaRepository<ExecutionStrategyInfo, Integer> {

}
