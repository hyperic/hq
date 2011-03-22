package org.hyperic.hq.escalation.data;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EscalationRepository extends JpaRepository<Escalation, Integer> {

    Escalation findByName(String name);
}
