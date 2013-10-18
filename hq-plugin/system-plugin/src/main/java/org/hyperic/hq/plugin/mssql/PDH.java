/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.mssql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author Administrator
 */
public class PDH {

    private static final Log log = LogFactory.getLog(PDH.class);

    private static native long pdhOpenQuery() throws PluginException;

//    private static native void pdhCloseQuery(long query) throws PluginException;
    private static native long pdhAddCounter(long query, String path) throws PluginException;

    private static native void pdhRemoveCounter(long counter) throws PluginException;

    private static native void PdhCollectQueryData(long query) throws PluginException;

    private static native double PdhGetFormattedCounterValue(long counter) throws PluginException;

    private static native String[] pdhGetInstances(String path) throws PluginException;

    private static final class InstanceIndex {

        long index = 0;
    }

    public static String[] getInstances(String path) throws PluginException {
        String[] instances = pdhGetInstances(path);

        /* PdhEnumObjectItems() does not include the instance index */
        HashMap names = new HashMap(instances.length);
        for (int i = 0; i < instances.length; i++) {
            InstanceIndex ix = (InstanceIndex) names.get(instances[i]);
            if (ix == null) {
                ix = new InstanceIndex();
                names.put(instances[i], ix);
            } else {
                ix.index++;
                instances[i] = instances[i] + "#" + ix.index;
            }
        }

        return instances;
    }

    public static Map<String, Double> getFormattedValues(List<String> paths) throws PluginException, InterruptedException {
        Map<String, Double> res = new HashMap<String, Double>();
        long q = PDH.pdhOpenQuery();

        Map<String, Long> counters = new HashMap<String, Long>();

        for (String path : paths) {
            try {
                counters.put(path, pdhAddCounter(q, path));
            } catch (PluginException ex) {
                log.debug("[getFormattedValues] Error adding metric => path:'" + path + "' ex:" + ex);
                res.put(path, Double.NaN);
            }
        }

        if (counters.size() > 0) {
            try {
                PdhCollectQueryData(q);
                Thread.sleep(1000);
                PdhCollectQueryData(q);

                for (String path : paths) {
                    try {
                        Long c = counters.get(path);
                        if (c != null) {
                            Double val = PdhGetFormattedCounterValue(c);
                            log.debug("[getFormattedValues] path:'" + path + "' val:" + val);
                            res.put(path, val);
                        }
                    } catch (PluginException ex) {
                        log.debug("[getFormattedValues] Error getting metric value => path:'" + path + "' ex:" + ex);
                    }
                }
            } finally {
                for (Long counter : counters.values()) {
                    try {
                        pdhRemoveCounter(counter);
                    } catch (PluginException ex) {
                        log.debug("[getFormattedValues] Error removing counter => ex:" + ex);
                    }
                }
//                pdhCloseQuery(q);
            }
        }

        return res;
    }

    public static double getValue(String path) throws PluginException {
        double res = MetricValue.NONE.getValue();

        long q = PDH.pdhOpenQuery();
        long counter = pdhAddCounter(q, path);
        try {
            PdhCollectQueryData(q);
            res = PdhGetFormattedCounterValue(counter);
        } finally {
            pdhRemoveCounter(counter);
//            pdhCloseQuery(q);
        }
        return res;
    }

    static {
        String os = System.getProperty("os.arch");
        log.debug("[static] os: " + os);

        String libName = "system_pdh.dll";
        
        String libPathInJar = "/priv_lib/";
        libPathInJar += os.contains("64") ? "x64/" : "win32/";
//        libPathInJar += log.isDebugEnabled() ? "Debug/" : "Release/";
        libPathInJar += libName;
        
       
        try {
            URL in = PDH.class.getClassLoader().getResource(libPathInJar);
            if (in == null) {
                throw new FileNotFoundException(libPathInJar);
            }

            File out = new File(System.getProperty("java.io.tmpdir") , libName);
            log.info("[static] Reading dll fom: " + in);
            log.info("[static] Writing dll to: " + out.getAbsolutePath());
            InputStream inStream = in.openStream();
            OutputStream outStream = new FileOutputStream(out);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }

            inStream.close();
            outStream.close();

            System.load(out.getAbsolutePath());
        } catch (Exception e) {
            log.error(e, e);
        }
    }
}
