package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GtriggerTypeInfoRepository extends JpaRepository<GtriggerTypeInfo, Integer> {

    GtriggerTypeInfo findByType(Class<?> type);

}
