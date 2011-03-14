package org.hyperic.hq.measurement.data;

import java.util.List;

import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorableTypeRepository extends JpaRepository<MonitorableType, Integer> {

    List<MonitorableType> findByPluginName(String plugin);
}
