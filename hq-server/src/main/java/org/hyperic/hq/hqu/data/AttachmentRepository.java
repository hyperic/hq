package org.hyperic.hq.hqu.data;

import java.util.List;

import org.hyperic.hq.hqu.server.session.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends JpaRepository<Attachment, Integer>{
    
    @Query("select a from Attachment a where a.view.attachTypeEnum = :typeCode")
    List<Attachment> findFor(@Param("typeCode") int typeCode);
    
}
