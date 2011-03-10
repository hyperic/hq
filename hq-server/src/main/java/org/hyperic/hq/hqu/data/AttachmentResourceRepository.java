package org.hyperic.hq.hqu.data;

import java.util.List;

import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.AttachmentResource;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AttachmentResourceRepository extends JpaRepository<AttachmentResource, Integer> {

    // TODO previously accepted Resource prototypes for type-based attachment
    @Transactional(readOnly=true)
    @Query("select a from AttachmentResource a where a.resource = :resource and a.category = :cat")
    List<Attachment> findFor(@Param("resource") Resource resource, @Param("cat") String category);
}
