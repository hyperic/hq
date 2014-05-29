/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.system;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.Metric;

/**
 *
 * @author glaullon
 */
public class LinuxCollector extends Collector {

    private static final Log log = LogFactory.getLog(LinuxCollector.class);
    private final Map<String, DeviceStats> sdpMap = new HashMap<String, DeviceStats>();

    @Override
    public void collect() {
        log.debug("[collect]");
        List<String[]> stats = LinuxUtils.getBlockDevicesStats();
        for (String[] fields : stats) {
            String dev = fields[LinuxUtils.DS_DEV_NAME];
            DeviceStats sdc = new DeviceStats();
            DeviceStats sdp = sdpMap.get(dev);
            if(sdp==null){
                sdp = new DeviceStats();
            }

            int rd_ios = Integer.parseInt(fields[LinuxUtils.rd_ios]);
            int wr_ios = Integer.parseInt(fields[LinuxUtils.wr_ios]);
            int tot_ticks = Integer.parseInt(fields[LinuxUtils.tot_ticks]);
            int rq_ticks = Integer.parseInt(fields[LinuxUtils.rq_ticks]);
            
            sdc.nr_ios = rd_ios + wr_ios;
            sdc.rd_sect = Integer.parseInt(fields[LinuxUtils.rd_sect]);
            sdc.wr_sect = Integer.parseInt(fields[LinuxUtils.wr_sect]);
            sdc.rd_ticks = Integer.parseInt(fields[LinuxUtils.rd_ticks]);
            sdc.wr_ticks = Integer.parseInt(fields[LinuxUtils.wr_ticks]);

            double arqsz = 0;
            double await = 0;
            if ((sdc.nr_ios - sdp.nr_ios) != 0) {
                arqsz = ((sdc.rd_sect - sdp.rd_sect) + (sdc.wr_sect - sdp.wr_sect)) / ((double) (sdc.nr_ios - sdp.nr_ios));
                await = ((sdc.rd_ticks - sdp.rd_ticks) + (sdc.wr_ticks - sdp.wr_ticks)) / ((double) (sdc.nr_ios - sdp.nr_ios));
            }
            
            setValue(dev + ".tps", sdc.nr_ios);
            setValue(dev + ".rd_ios", rd_ios);
            setValue(dev + ".wr_ios", wr_ios);
            setValue(dev + ".arqsz", arqsz);
            setValue(dev + ".await", await);
            setValue(dev + ".tot_ticks", tot_ticks);
            setValue(dev + ".rq_ticks", rq_ticks/1000);
            setValue(dev + ".Availability", Metric.AVAIL_UP);

            sdpMap.put(dev, sdc);
        }
    }

    private class DeviceStats {

        private double nr_ios;
        private double rd_sect;
        private double wr_sect;
        private double rd_ticks;
        private double wr_ticks;

    }
}
