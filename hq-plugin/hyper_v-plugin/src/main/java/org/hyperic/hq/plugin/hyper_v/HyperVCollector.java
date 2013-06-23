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
        try {
            String[] instances = Pdh.getInstances("Hyper-V Hypervisor Logical Processor");
            Set<String> lpNames = new HashSet<String>();
            for (int i = 0; i < instances.length; i++) {
                String instance = instances[i];
                if ("_Total".equals(instance) || "<All instances>".equals(instance)) {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(instance,":");
                String lpName = (String) st.nextElement();
                lpNames.add(lpName);
            }
            Pdh pdh;
            pdh = new Pdh();
            for (Iterator<String> it = lpNames.iterator(); it.hasNext();) {
                setLPMetricVal(pdh,it.next(),"% Guest Run Time");
                setLPMetricVal(pdh,it.next(),"% Hypervisor Run Time");
                setLPMetricVal(pdh,it.next(),"% Idle Time");
                setLPMetricVal(pdh,it.next(),"% Total Run Time");
            }
        }catch(Win32Exception e) {
//~~~~~~~~~~~~~~~~
            e.printStackTrace();
        }
    }


    private void setLPMetricVal(Pdh pdh,String lpName, String metricName) throws Win32Exception {
        String lpPath = "\\Hyper-V Hypervisor Logical Processor(" + lpName + ")\\";
        double val = pdh.getFormattedValue(lpPath + metricName);
        setValue(metricName, val);
    }
}
