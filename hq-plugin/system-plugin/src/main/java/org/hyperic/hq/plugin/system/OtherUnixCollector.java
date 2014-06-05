/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;

/**
 *
 * @author glaullon
 */
public class OtherUnixCollector extends Collector {

    private static final Log log = LogFactory.getLog(OtherUnixCollector.class);
    private static final String VMSTAT[] = {"vmstat", "-s"};
    private static final String METRICS[][] = {
        {"pgfault", "minor (as) faults"}, {"pgmajfault", "major faults"}, // Solaris
        {"pgfault", "total address trans. faults"}, {"pgmajfault", "executable filled pages faults"}, // AIX
        {"pgfault", "total address trans. faults taken"}, {"pgmajfault", "executable fill page faults"}, // HPUX
        
    };

    @Override
    public void collect() {
        log.info("[collect]");
        Process cmd;
        try {
            cmd = Runtime.getRuntime().exec(VMSTAT);
            cmd.waitFor();
            if (cmd.exitValue() != 0) {
                String msg = inputStreamAsString(cmd.getErrorStream());
                log.info("[collect] cmd error: " + msg);
                setErrorMessage(msg);
            } else {
                Map<String, Double> stats = parseVMStat(cmd.getInputStream());
                for (String[] metric : METRICS) {
                    final Double val = stats.get(metric[1]);
                    log.info("[collect] " + metric[0] + "=" + val);
                    if (val != null) {
                        setValue(metric[0], val);
                    }
                }
            }
        } catch (IOException e) {
            log.info("[collect] error: " + e.getMessage(), e);
            setErrorMessage(e.getLocalizedMessage(), e);
        } catch (InterruptedException e) {
            log.info("[collect] error: " + e.getMessage(), e);
            setErrorMessage(e.getLocalizedMessage(), e);
        }
    }

    static final Map<String, Double> parseVMStat(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        Map<String, Double> res = new HashMap<String, Double>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                int sep = line.indexOf(" ");
                log.info("[parseVMStat] line='" + line + "' (" + sep + ")");
                if (sep > 0) {
                    String key = line.substring(sep).trim();
                    String val = line.substring(0, sep).trim();
                    log.info("[parseVMStat] " + key + " = " + val);
                    res.put(key, Double.parseDouble(val));
                }
            }
        } finally {
            br.close();
        }
        return res;
    }

    static final String inputStreamAsString(InputStream stream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }
}
