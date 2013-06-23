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
            //                Set<Map<String,String>> objects = new HashSet<Map<String,String>>();
            //                Map<String,String> obj = null;
            //                while ((line = input.readLine()) != null) {
            //                    line = line.trim();
            //                    if (line.isEmpty()||line.isEmpty()||obj==null) {
            //                        obj = new HashMap<String,String>();
            //                        objects.add(obj);
            //                    }
            //                    StringTokenizer st = new StringTokenizer(line,":");
            //                    while (st.hasMoreElements()) {
            //                        String k = ((String) st.nextElement()).trim();
            //                        String v = ((String) st.nextElement()).trim();
            //                        obj.put(k,v);
            //                    }
            //                }
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
        } else {
            return super.getValue(metric);
        }
    }
}
