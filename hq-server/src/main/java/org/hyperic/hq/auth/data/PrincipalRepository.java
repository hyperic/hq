package org.hyperic.hq.auth.data;

import org.hyperic.hq.auth.Principal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrincipalRepository extends JpaRepository<Principal, Integer> {

    Principal findByPrincipal(String principal);
}
