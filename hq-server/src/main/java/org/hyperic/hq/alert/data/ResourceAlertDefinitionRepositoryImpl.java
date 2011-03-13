package org.hyperic.hq.alert.data;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.server.session.ResourceTypeAlertDefinition;

public class ResourceAlertDefinitionRepositoryImpl implements
    ResourceAlertDefinitionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    public int setChildrenActive(ResourceTypeAlertDefinition def, boolean active) {
        return entityManager
            .createQuery(
                "update ResourceAlertDefinition def set def.active = :active, "
                    + "def.enabled = :active, def.mtime = :mtime "
                    + "where def.resourceTypeAlertDefinition = :def")
            .setParameter("active", active).setParameter("mtime", System.currentTimeMillis())
            .setParameter("def", def).executeUpdate();
    }

    public int setChildrenEscalation(ResourceTypeAlertDefinition def, Escalation esc) {
        return entityManager
            .createQuery(
                "update ResourceAlertDefinition set escalation = :esc, "
                    + "mtime = :mtime where resourceTypeAlertDefinition = :def")
            .setParameter("esc", esc).setParameter("mtime", System.currentTimeMillis())
            .setParameter("def", def).executeUpdate();
    }

}
