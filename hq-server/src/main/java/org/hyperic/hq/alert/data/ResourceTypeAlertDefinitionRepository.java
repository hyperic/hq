package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.events.server.session.ResourceTypeAlertDefinition;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceTypeAlertDefinitionRepository extends
    JpaRepository<ResourceTypeAlertDefinition, Integer> {

    List<ResourceTypeAlertDefinition> findByEnabled(boolean enabled, Sort sort);

    List<ResourceTypeAlertDefinition> findByResourceType(ResourceType resourceType);
}
