package org.hyperic.hq.measurement.data;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface AvailabilityDataCustom {

    List<AvailabilityDataRLE> findByMeasurements(List<Integer> mids);

    List<AvailabilityDataRLE> getHistoricalAvails(Measurement m, long start, long end,
                                                  boolean descending);

    List<AvailabilityDataRLE> getHistoricalAvails(Integer[] mids, long start, long end,
                                                  boolean descending);

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
    Map<Integer, TreeSet<AvailabilityDataRLE>> getHistoricalAvailMap(Integer[] mids,
                                                                     final long after,
                                                                     final boolean descending);
    /**
     * @return List of Object[]. [0] = Measurement Obj [1] = min(availVal), [2]
     *         = max(availVal), [3] = avg(availVal) [4] = mid count, [5] = total
     *         uptime, [6] = = total time
     */
    List<Object[]> findAggregateAvailability(Integer[] mids, long start, long end);
    
    List<AvailabilityDataRLE> getDownMeasurements(List<Integer> includes);
}
