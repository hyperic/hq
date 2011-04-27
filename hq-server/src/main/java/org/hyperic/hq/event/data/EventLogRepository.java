package org.hyperic.hq.event.data;

import java.util.List;
import java.util.Set;

import org.hyperic.hq.events.server.session.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EventLogRepository extends JpaRepository<EventLog, Integer>,
    EventLogRepositoryCustom {

    @Transactional
    @Modifying
    @Query("delete EventLog l where l.resource = :resource")
    void deleteByResource(@Param("resource") Integer resource);

    // TODO this method used to do perm checks
    @Transactional(readOnly = true)
    @Query("select l from EventLog l where l.resource=:resource and l.timestamp between :begin "
           + "and :end and l.type in (:eventTypes) order by l.timestamp")
    List<EventLog> findByTimestampBetweenAndResourceAndEventTypesOrderByTimestamp(@Param("begin") long begin,
                                                                                  @Param("end") long end,
                                                                                  @Param("resource") Integer resource,
                                                                                  @Param("eventTypes") List<String> eventTypes);

    // TODO this method used to do perm checks
    List<EventLog> findByTimestampBetweenAndResourceOrderByTimestampAsc(long begin, long end,
                                                                        Integer resource);

    @Transactional(readOnly = true)
    @Query("select l from EventLog l where l.resource in (:resources) and l.timestamp between :begin and :end "
           + "and l.type in (:eventTypes) order by l.timestamp")
    List<EventLog> findByTimestampBetweenAndResourcesAndEventTypesOrderByTimestamp(@Param("begin") long begin,
                                                                                   @Param("end") long end,
                                                                                   @Param("resources") Set<Integer> resources,
                                                                                   @Param("eventTypes") List<String> eventTypes);

    @Transactional(readOnly = true)
    @Query("select l from EventLog l where l.resource in (:resources) and l.timestamp between :begin and :end "
           + "order by l.timestamp")
    List<EventLog> findByTimestampBetweenAndResourcesOrderByTimestamp(@Param("begin") long begin,
                                                                      @Param("end") long end,
                                                                      @Param("resources") Set<Integer> resources);

    // TODO this method used to do perm checks
    List<EventLog> findByTimestampBetweenAndStatusAndResourceOrderByTimestampAsc(long begin,
                                                                                 long end,
                                                                                 String status,
                                                                                 Integer resource);

    @Transactional(readOnly = true)
    @Query("select e FROM EventLog e WHERE e.timestamp >= :timestamp AND e.type = :type AND e.instanceId is not null")
    List<EventLog> findByTimestampGreaterThanOrEqualToAndType(@Param("timestamp") long timestamp,
                                                              @Param("type") String type);

}
