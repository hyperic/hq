package org.hyperic.hq.autoinventory.data;

import java.util.List;

import org.hyperic.hq.autoinventory.AIPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AIPlatformRepository extends JpaRepository<AIPlatform,Integer>{
    
    @Query("select a from AIPlatform a where a.ignored=false and a.lastApproved < a.modifiedTime order by name")
    List<AIPlatform> findAllNotIgnored();
    
    @Query("select a from AIPlatform a where a.ignored=false order by name")
    List<AIPlatform> findAllNotIgnoredIncludingProcessed();
    
    @Query("select a from AIPlatform a order by name")
    List<AIPlatform> findAllIncludingProcessed();
    
    AIPlatform findByFqdn(String fqdn);
    
    AIPlatform findByCertdn(String certdn);
    
    AIPlatform findByAgentToken(String token);

}
