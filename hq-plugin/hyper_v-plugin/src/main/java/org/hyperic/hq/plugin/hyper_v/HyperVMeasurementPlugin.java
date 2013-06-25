package org.hyperic.hq.plugin.hyper_v;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;

public class HyperVMeasurementPlugin extends Win32MeasurementPlugin {
    
    protected Map<String,String> execPowerShell(Metric metric) throws PluginException {
        String psCmd = metric.getObjectProperty("command");
        StringBuilder sb = new StringBuilder().append("cmd /C powershell \"").append(psCmd);

        String att = metric.getAttributeName();
        if (att!=null&&!"".equals(att)) {
            sb.append(" ").append(att);
        }

        String so = metric.getObjectProperty("columns");
        if (so!=null&&!"".equals(so)) {
            sb.append(" | Select-Object " + so);
        }

        sb.append(" | Format-List\"");
        String cmd = sb.toString();
        BufferedReader input = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            Map<String,String> obj = new HashMap<String,String>();
            while ((line = input.readLine()) != null) {
                line = line.trim();
                StringTokenizer st = new StringTokenizer(line,":");
                while (st.hasMoreElements()) {
                    String k = ((String) st.nextElement()).trim();
                    String v = ((String) st.nextElement()).trim();
                    obj.put(k,v);
                }
            }
            return obj;
        }catch(IOException e) {
            throw new PluginException(e);
        } finally {
            if (input!=null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new PluginException(e);
                }
            }
        }
    }

    @Override
    public MetricValue getValue(Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {
        if ("PowerShell".equals(metric.getDomainName())) {
            Map<String,String> obj = execPowerShell(metric);
            String state = obj.get("State");
            if ("Running".equals(state)) {
                return new MetricValue(Metric.AVAIL_UP);
            } else {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
        } else if ("pdh".equals(metric.getDomainName())) {
            String obj = "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName();
            Double val;
            try {
                val = new Pdh().getFormattedValue(obj.replaceAll("%3A", ":"));
                return new MetricValue(val);
            }catch(Win32Exception e) {
                throw new PluginException(e);
            }
        } else {
            return super.getValue(metric);
        }
    }
}
