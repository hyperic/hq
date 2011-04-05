package org.hyperic.hq.auth.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;

public class AuthzSubjectRepositoryImpl implements AuthzSubjectRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public AuthzSubject findOwner(Resource resource) {
        List<Integer> subjectIds = (List<Integer>) entityManager
            .createNativeQuery("select SUBJECT_ID from OWNED_RESOURCES where RESOURCE_ID=:resource")
            .setParameter("resource", resource.getId()).setMaxResults(1).getResultList();
        if (subjectIds.isEmpty()) {
            return null;
        }
        return entityManager.find(AuthzSubject.class, subjectIds.get(0));

    }

    @SuppressWarnings("unchecked")
    public Set<Resource> getOwnedResources(AuthzSubject subject) {
        List<Integer> resourceIds = (List<Integer>) entityManager
            .createNativeQuery("select RESOURCE_ID from OWNED_RESOURCES where SUBJECT_ID=:subject")
            .setParameter("subject", subject.getId()).getResultList();
        Set<Resource> resources = new HashSet<Resource>();
        for (Integer resourceId : resourceIds) {
            resources.add(entityManager.find(Resource.class, resourceId));
        }
        return resources;
    }

    public void setOwner(AuthzSubject subject, Resource resource) {
        entityManager
            .createNativeQuery(
                "insert into OWNED_RESOURCES(SUBJECT_ID,RESOURCE_ID) values(:subject,:resource)")
            .setParameter("subject", subject.getId()).setParameter("resource", resource.getId())
            .executeUpdate();
    }

    public void removeOwner(AuthzSubject subject, Resource resource) {
        entityManager
            .createNativeQuery(
                "delete from OWNED_RESOURCES where SUBJECT_ID=:subject and RESOURCE_ID=:resource")
            .setParameter("subject", subject.getId()).setParameter("resource", resource.getId())
            .executeUpdate();
    }
}
