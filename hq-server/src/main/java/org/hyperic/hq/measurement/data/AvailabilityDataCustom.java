package org.hyperic.hq.measurement.data;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface AvailabilityDataCustom {

    /**
     * @return List of Object[]. [0] = Measurement Obj [1] = min(availVal), [2]
     *         = max(availVal), [3] = avg(availVal) [4] = mid count, [5] = total
     *         uptime, [6] = = total time
     */
    List<Object[]> findAggregateAvailability(List<Integer> mids, long start, long end);

    List<AvailabilityDataRLE> findLastByMeasurements(List<Integer> mids);

    List<AvailabilityDataRLE> getDownMeasurements(List<Integer> includes);

    /**
     * @return {@link Map} of {@link Integer} to ({@link TreeSet} of
     *         {@link AvailabilityDataRLE}).
     *         <p>
     *         The {@link Map} key of {@link Integer} == {@link Measurement}
     *         .getId().
     *         <p>
     *         The {@link TreeSet}'s comparator sorts by
     *         {@link AvailabilityDataRLE}.getStartime().
     */
    Map<Integer, TreeSet<AvailabilityDataRLE>> getHistoricalAvailMap(List<Integer> mids,
                                                                     final long after,
                                                                     final boolean descending);

    List<AvailabilityDataRLE> getHistoricalAvails(List<Integer> mids, long start, long end,
                                                  boolean descending);

    List<AvailabilityDataRLE> getHistoricalAvails(Measurement m, long start, long end,
                                                  boolean descending);

    List<AvailabilityDataRLE> getHistoricalAvails(Resource resource, long start, long end);
}
