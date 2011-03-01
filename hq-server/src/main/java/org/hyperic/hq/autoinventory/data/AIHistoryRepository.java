package org.hyperic.hq.autoinventory.data;

import java.util.List;

import org.hyperic.hq.autoinventory.AIHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIHistoryRepository extends JpaRepository<AIHistory,Integer>  {

    List<AIHistory> findByEntityTypeAndEntityId(int type, int id);
    
    //TODO findByEntityTypeAndIdOrderByStartTime(either asc or desc), also: status, duration, dateScheduled   
  
}
