package org.hyperic.hq.dashboard.data;

import javax.persistence.QueryHint;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserDashboardConfigRepository extends JpaRepository<UserDashboardConfig, Integer> {

    @Modifying
    @Transactional
    @Query("delete from UserDashboardConfig u where u.user = :user")
    void deleteByUser(@Param("user") AuthzSubject user);

    @Transactional(readOnly = true)
    @Query("select u from UserDashboardConfig u where u.user = :user")
    @QueryHints({ @QueryHint(name = "org.hibernate.cacheable", value = "true"),
                 @QueryHint(name = "org.hibernate.cacheRegion", value = "UserDashboardConfig.findDashboard") })
    UserDashboardConfig findByUser(@Param("user") AuthzSubject user);
}
