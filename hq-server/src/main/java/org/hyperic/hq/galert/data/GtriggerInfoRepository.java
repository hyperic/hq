package org.hyperic.hq.galert.data;

import org.hyperic.hq.galerts.server.session.GtriggerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GtriggerInfoRepository extends JpaRepository<GtriggerInfo, Integer> {

}
