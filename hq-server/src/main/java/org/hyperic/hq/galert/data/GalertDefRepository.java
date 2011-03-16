package org.hyperic.hq.galert.data;

import java.util.List;

import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalertDefRepository extends JpaRepository<GalertDef, Integer> {

    List<GalertDef> findByGroup(ResourceGroup group);
}
