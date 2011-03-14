package org.hyperic.hq.update.data;

import org.hyperic.hq.bizapp.server.session.UpdateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateStatusRepository extends JpaRepository<UpdateStatus, Integer>{

}
