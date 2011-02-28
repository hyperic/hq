package org.hyperic.hq.autoinventory.data;

import java.util.List;

import org.hyperic.hq.autoinventory.AIServer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIServerRepository extends JpaRepository<AIServer,Integer>{
    
    List<AIServer> findByAIPlatform(Integer platformid);
    
    AIServer findByName(String name);
}
