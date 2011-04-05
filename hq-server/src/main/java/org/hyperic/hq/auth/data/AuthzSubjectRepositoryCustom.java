package org.hyperic.hq.auth.data;

import java.util.Set;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.transaction.annotation.Transactional;

public interface AuthzSubjectRepositoryCustom {

    @Transactional(readOnly = true)
    AuthzSubject findOwner(Resource resource);

    @Transactional(readOnly = true)
    Set<Resource> getOwnedResources(AuthzSubject subject);

    @Transactional
    void removeOwner(AuthzSubject subject, Resource resource);

    @Transactional
    void setOwner(AuthzSubject subject, Resource resource);
}
