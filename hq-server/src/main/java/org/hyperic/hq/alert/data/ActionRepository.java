package org.hyperic.hq.alert.data;

import java.util.List;

import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ActionRepository extends JpaRepository<Action, Integer>, ActionRepositoryCustom {

    @Transactional(readOnly = true)
    @Query("select a from Action a, AlertActionLog al "
           + "where a.id = al.action AND al.alert = :alert")
    List<Action> findByAlert(@Param("alert") Alert alert);
}
