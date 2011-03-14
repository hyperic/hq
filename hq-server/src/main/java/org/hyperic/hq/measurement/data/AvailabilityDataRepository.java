package org.hyperic.hq.measurement.data;

import org.hyperic.hq.measurement.server.session.AvailabilityDataId;
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityDataRepository extends JpaRepository<AvailabilityDataRLE, AvailabilityDataId>,
    AvailabilityDataCustom {

}
