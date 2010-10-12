package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.ext.ProblemMetricInfo;
import org.hyperic.hq.measurement.ext.ProblemResourceInfo;
import org.hyperic.hq.measurement.shared.ProblemMetricManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;

import edu.emory.mathcs.backport.java.util.Collections;

public class ProblemMetricManagerImpl implements ProblemMetricManager {

    @Override
    public void createProblem(Integer mid, long time, int type, Integer additional) {
    }

    @Override
    public MetricProblem getByIdAndTimestamp(Measurement meas, long timestamp) {
        return null;
    }

    @Override
    public ProblemMetricInfo[] getProblemMetrics(AppdefEntityID aid, long begin, long end) {
        return new ProblemMetricInfo[0];
    }

    @Override
    public ProblemResourceInfo[] getProblemResources(long begin, long end,
                                                     Set<AppdefEntityID> permitted,
                                                     PageControl pc) {
        return new ProblemResourceInfo[0];
    }

    @Override
    public ProblemResourceInfo[] getProblemResourcesByTypeAndInstances(
        int appdefType, int[] instanceIds, long begin, long end, PageControl pc) {
        return new ProblemResourceInfo[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Integer, ProblemMetricInfo> getProblemsByTemplate(
        int appdefType, Integer[] eids, long begin, long end) {
        return Collections.emptyMap();
    }

    @Override
    public void processMetricValue(Integer mid, MetricValue mv, int type) {
    }

    @Override
    public void removeProblems(Collection<Integer> mids) {
    }

    @Override
    public void removeProblems(AppdefEntityID entityId) {
    }

}
