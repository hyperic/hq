package org.hyperic.hq.autoinventory.data;

import java.util.List;

import org.hyperic.hq.autoinventory.AIIp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIIpRepository extends JpaRepository<AIIp,Integer> {

    List<AIIp> findByAddress(String addr);
    
    List<AIIp> findByMacAddress(String addr);
}
