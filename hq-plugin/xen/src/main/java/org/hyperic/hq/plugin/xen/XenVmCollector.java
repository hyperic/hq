package org.hyperic.hq.plugin.xen;

import java.util.Iterator;
import java.util.Map;

import org.hyperic.hq.product.Metric;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VMMetrics;
import com.xensource.xenapi.Types.VmPowerState;

public class XenVmCollector extends XenCollector {

    public void collect() {
        try {
            Connection conn = connect();
            VM vm = VM.getByUuid(conn, getServerUUID());
            double avail;
            VmPowerState state = vm.getPowerState(conn);
            if (state == VmPowerState.RUNNING) {
                avail = Metric.AVAIL_UP;                
            }
            else if ((state == VmPowerState.PAUSED) ||
                     (state == VmPowerState.SUSPENDED))
            {
                avail = Metric.AVAIL_PAUSED;
            }
            else {
                avail = Metric.AVAIL_DOWN;
            }
            setAvailability(avail);
            VMMetrics metrics = vm.getMetrics(conn);
            VMMetrics.Record record = metrics.getRecord(conn);
            setValue("MemoryActual", record.memoryActual);
            long startTime = record.startTime.getTime();
            setValue("StartTime", startTime);
            setValue("Uptime", System.currentTimeMillis() - startTime);
            Map<Long, Double> cpus = record.VCPUsUtilisation;
            double usage = 0;
            for (Iterator it = cpus.values().iterator();
                 it.hasNext();)
            {
                usage += ((Double)it.next()).doubleValue();
            }
            setValue("CPUUsage", usage);
        } catch (Exception e) {
            setAvailability(false);
            setErrorMessage(e.getMessage());
        }
    }
}
