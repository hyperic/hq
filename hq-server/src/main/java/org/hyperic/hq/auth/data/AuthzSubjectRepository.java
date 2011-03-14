package org.hyperic.hq.auth.data;

import javax.persistence.QueryHint;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AuthzSubjectRepository extends JpaRepository<AuthzSubject, Integer> {

    @Transactional(readOnly = true)
    @Query("select s from AuthzSubject s where s.name=:name and s.dsn=:dsn")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "AuthzSubject.findByName") })
    AuthzSubject findByName(@Param("name") String name);

    @Transactional(readOnly = true)
    @Query("select s from AuthzSubject s where s.name=:name and s.dsn=:dsn")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "AuthzSubject.findByAuth") })
    AuthzSubject findByNameAndDsn(@Param("name") String name, @Param("dsn") String dsn);

    @Transactional(readOnly = true)
    @Query("select s from AuthzSubject s join s.ownedResources r where r=:resource")
    AuthzSubject findOwner(@Param("resource") Resource resource);

}
