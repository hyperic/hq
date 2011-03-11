package org.hyperic.hq.dashboard.data;

import java.util.List;

import javax.persistence.QueryHint;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.auth.domain.Role;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RoleDashboardConfigRepository extends JpaRepository<RoleDashboardConfig, Integer> {

    @Modifying
    @Transactional
    @Query("delete from RoleDashboardConfig r where r.role = :role")
    void deleteByRole(@Param("role") Role role);

    @Transactional(readOnly = true)
    @Query("select r from RoleDashboardConfig r order by r.name")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "RoleDashboardConfig.findAllRoleDashboards") })
    List<RoleDashboardConfig> findAllOrderByName();

    @Transactional(readOnly = true)
    @Query("select r from RoleDashboardConfig r where r.role = :role")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "RoleDashboardConfig.findDashboard") })
    RoleDashboardConfig findByRole(@Param("role") Role role);

    @Transactional(readOnly = true)
    @Query("select r from RoleDashboardConfig r where :user in elements(r.role.subjects)")
    List<RoleDashboardConfig> findByUser(@Param("user") AuthzSubject user);
}
