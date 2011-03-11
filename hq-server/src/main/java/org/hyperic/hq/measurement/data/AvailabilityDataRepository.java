package org.hyperic.hq.measurement.data;

import java.util.List;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityDataRepository extends JpaRepository<AvailabilityDataRLE, Integer>,
    AvailabilityDataCustom {

    List<AvailabilityDataRLE> findByResourceAndEndtimeGreaterThanAndStarttimeLessThanOrderByStarttime(Resource resource, long start, long end);
}
