package org.hyperic.hq.measurement.data;

import org.hyperic.hq.measurement.server.session.MeasurementDataId;
import org.hyperic.hq.measurement.server.session.MetricProblem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MetricProblemRepository extends JpaRepository<MetricProblem, MeasurementDataId>, MetricProblemRepositoryCustom {

}
