package org.hyperic.hq.agent.mgmt.data;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

public class ManagedResourceRepositoryImpl implements ManagedResourceRepositoryCustom {

    @Autowired
    private JpaTemplate jpaTemplate;

    @SuppressWarnings("unchecked")
    @PostConstruct
    public void loadFindByManagedResourceQueryCache() {
        jpaTemplate.execute(new JpaCallback() {
            public Object doInJpa(EntityManager entityManager) {
                List<Integer> resourceIds = entityManager.createQuery(
                    "select m.resourceId from ManagedResource m", Integer.class).getResultList();
                for (Integer resourceId : resourceIds) {
                    entityManager
                        .createQuery(
                            "select m.agent from ManagedResource m where m.resourceId=:resource",
                            Agent.class).setHint("org.hibernate.cacheable", true)
                        .setHint("org.hibernate.cacheRegion", "Agent.findByManagedResource")
                        .setParameter("resource", resourceId).getSingleResult();
                }
                return null;
            }
        });
    }

}
