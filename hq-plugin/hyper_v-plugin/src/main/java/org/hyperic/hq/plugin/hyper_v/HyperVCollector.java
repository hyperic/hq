package org.hyperic.hq.plugin.hyper_v;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.hyperic.hq.product.Collector;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVCollector extends Collector {

    public HyperVCollector() {
    }

    
    @Override
    public void collect() {
        Pdh pdh;
        try {
            pdh = new Pdh();
            collectAllInstances(pdh,"Hyper-V Hypervisor Logical Processor","% Guest Run Time","% Hypervisor Run Time","% Idle Time","% Total Run Time");
            collectAllInstances(pdh,"Network Interface","Bytes Total/sec","Offloaded Connections","Packets/sec","Packet Outbound Errors","Packet Receive Errors");
            collectAllInstances(pdh,"PhysicalDisk","Current Disk Queue Length","Disk Bytes/sec","Disk Transfers/sec");
        }catch(Win32Exception e) {
          //~~~~~~~~~~~~~~~~
            e.printStackTrace();
        }
    }
    protected void collectAllInstances(Pdh pdh, String propertySet, String ... metricNames) throws Win32Exception {
        String[] instances = Pdh.getInstances(propertySet);
        Set<String> instancesNames = new HashSet<String>();
        for (int i = 0; i < instances.length; i++) {
            String instance = instances[i];
            if ("_Total".equals(instance) || "<All instances>".equals(instance)) {
                continue;
            }
            StringTokenizer st = new StringTokenizer(instance,":");
            String instanceName = (String) st.nextElement();
            instancesNames.add(instanceName);
        }
        for (Iterator<String> it = instancesNames.iterator(); it.hasNext();) {
            String instanceName = it.next();
            for(String metricName:metricNames) {
                setLPMetricVal(pdh,propertySet,instanceName,metricName);
            }
        }
    }

    private void setLPMetricVal(Pdh pdh,String propertySet,String instanceName, String metricName) throws Win32Exception {
        String metricFullPath = "\\"+propertySet+"(" + instanceName + ")\\";
        double val = pdh.getFormattedValue(metricFullPath + metricName);
        setValue(metricName, val);
    }
}
