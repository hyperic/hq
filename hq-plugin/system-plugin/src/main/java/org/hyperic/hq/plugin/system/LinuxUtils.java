/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author glaullon
 */
public class LinuxUtils {

    public static final int DS_DEV_NAME = 2; // 3 - device name
    public static final int rd_ios = 3; // 4 - reads completed successfully
    public static final int rd_sect = 4; // 5 - reads merged
    public static final int rd_ticks = 6; // 7 - time spent reading (ms)
    public static final int wr_ios = 7; // 8 - writes completed
    public static final int wr_sect = 9; // 9 - writes merged
    public static final int wr_ticks = 10; // 11 - time spent writing (ms)
    public static final int tot_ticks = 12; // 13 - time spent doing I/Os (ms)
    public static final int rq_ticks = 13; // 14 - weighted time spent doing I/Os (ms)

    private static final File DISKSTATS = new File("/proc/diskstats");
    private static final File VMSTAT = new File("/proc/vmstat");
    private static final Log log = LogFactory.getLog(LinuxUtils.class);

    public static List<String> getBlockDevicesList() {
        List<String> res = new ArrayList<String>();
        List<String[]> stats = readFile(DISKSTATS);
        for (String[] fields : stats) {
            File blockDevice = new File("/sys/block/" + fields[DS_DEV_NAME] + "/device/");
            int reads = Integer.parseInt(fields[rd_ios]);
            if (blockDevice.canRead() && (reads > 0)) {
                res.add(fields[DS_DEV_NAME]);
            }
        }
        return res;
    }

    public static List<String[]> getBlockDevicesStats() {
        List<String[]> res = new ArrayList<String[]>();
        List<String[]> stats = readFile(DISKSTATS);
        for (String[] fields : stats) {
            int reads = Integer.parseInt(fields[rd_ios]);
            if (reads > 0) {
                res.add(fields);
            }
        }
        return res;
    }

    public static Map<String, Integer> getVMStats() {
        Map<String, Integer> res = new HashMap<String, Integer>();
        List<String[]> stats = readFile(VMSTAT);
        for (String[] fields : stats) {
            if (fields.length == 2) {
                res.put(fields[0], Integer.parseInt(fields[1]));
            }
        }
        return res;
    }

    private static List<String[]> readFile(File file) {
        List<String[]> res = new ArrayList<String[]>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.trim().split("\\s+");
                res.add(fields);
            }
        } catch (IOException ex) {
            log.debug("[readFile] " + ex, ex);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                log.debug("[readFile] " + ex, ex);
            }
        }
        return res;
    }
}
