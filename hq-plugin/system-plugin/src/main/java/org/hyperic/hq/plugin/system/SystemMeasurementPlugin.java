/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.system;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hyperic.hq.measurement.data.TopNConfigurationProperties;
import org.hyperic.hq.plugin.mssql.PDH;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.LogTrackPlugin;
import static org.hyperic.hq.product.MeasurementPlugin.TYPE_COLLECTOR;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;
import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class SystemMeasurementPlugin
    extends SigarMeasurementPlugin
{
    private void reportError(Metric metric, Exception e) {
        getLog().error(metric + ": " + e.getMessage(), e);
        getManager().reportEvent(metric,
                                 System.currentTimeMillis(),
                                 LogTrackPlugin.LOGLEVEL_ERROR,
                                 "system", e.getMessage());
    }

    private MetricValue getPdhValue(Metric metric) throws PluginException {
        String obj = "\\" + metric.getObjectPropString() + "\\" + metric.getAttributeName();
        Double val;
        try 
        {   
            val = new Pdh().getRawValue(obj);
            return new MetricValue(val);

        }catch(Win32Exception e) {
            throw new PluginException(e);
    }
    }
    
    private MetricValue getPdhValue2(Metric metric) {
        String prop = metric.getObjectPropString().replaceAll("%3A", ":");
        prop = prop.replaceAll("%26", "&");

        String obj = "\\" + prop + "\\";
        if (metric.getAttributeName().startsWith("Memory ")) {
            obj += metric.getAttributeName().replace("Memory ", "");
        } else {
            obj += metric.getAttributeName();
        }
        getLog().debug("[getPdhValue2] obj=" + obj);

        Double val = Double.NaN;
        try {
            val = PDH.getValue(obj);
        } catch (PluginException e) {
            getLog().debug("[getPdhValue2] obj=" + obj + " error:"+e,e);
        }
        return new MetricValue(val);
    }

    @Override
    public MetricValue getValue(Metric metric)
            throws PluginException,
            MetricNotFoundException,
            MetricUnreachableException {
        getLog().debug("[getValue] '" + getTypeInfo().getName() + "' metric:" + metric);
        //nira: should be first!! since Type is not configured   
        String domain = metric.getDomainName();
        if (domain.equals("pdh2")) {
            if ("formated".equals(metric.getObjectProperties().get("type"))) {
                return Collector.getValue(this, metric);
            } else {
                return getPdhValue2(metric);
            }
        }

        if (domain.equalsIgnoreCase("calculated")) {
            double res = 0;
            try {
                FileSystem[] fslist = getSigar().getFileSystemList();
                for (FileSystem fs : fslist) {
                    if (fs.getType() == FileSystem.TYPE_LOCAL_DISK) {
                        getLog().debug("[calculated] fs: " + fs.getDirName());
                        FileSystemUsage fsu = getSigar().getMountedFileSystemUsage(fs.getDirName());
                        if (metric.getAttributeName().equals("capacity")) {
                            res += fsu.getTotal();
                        } else if (metric.getAttributeName().equals("usage")) {
                            res += fsu.getUsed();
                        } else {
                            getLog().debug("[calculated] " + metric.getAttributeName() + " not supported.");
                            return MetricValue.NONE;
                        }
                    }
                }
            } catch (SigarException ex) {
                getLog().debug(ex, ex);
            }
            return new MetricValue(res);
        }

        if (domain.equals("disk.avail")) {
            return getPhysicalDiskAvail(metric);
        }

        if (domain.equals("pdh")) {
            return getPdhValue(metric);
        }

        Properties props = metric.getObjectProperties();
        String type = props.getProperty("Type");

        // TalG: Fix HHQ-5967- check for null
        if (type == null){
            String errMsg = "No type for metric [" + metric.getAttributeName() + "]";
            throw new MetricNotFoundException(errMsg);
        }
        
        boolean isFsUsage = type.endsWith("FileSystemUsage");

        if (domain.equals("sigar.ext")) {
            if (type.equals("CpuInfo")) {
                return new MetricValue(getNumCpus());
            }
            else if (type.equals("DiskUsage")) {
                //back-compat w/ old template
                String arg = props.getProperty("Arg");
                return new MetricValue(getDiskUsage(arg));
            } else if (type.equals("ChildProcesses")) {
                String arg = props.getProperty("Arg");
                return new MetricValue(getChildProcessCount(arg));
            }
            else if (type.equals("FileSystemInfo")) {
                return new MetricValue(getAverageReadWrites());
            }
        }
        //else better be "system.avail"

        if (type.equals("Platform")) {
            return new MetricValue(Metric.AVAIL_UP);
        }

        if (type.equals("NetIfconfig")) {
            double avail;

            int value = (int)super.getValue(metric).getValue();
            if (((value & NetFlags.IFF_UP) > 0) && ((value & NetFlags.IFF_RUNNING) == NetFlags.IFF_RUNNING)) {
                avail = Metric.AVAIL_UP;
            } else if ((value & NetFlags.IFF_UP) > 0 ) {
                avail = Metric.AVAIL_WARN;
            } else {
                avail = Metric.AVAIL_DOWN;
            }

            return new MetricValue(avail);
        }

        if (isFsUsage) {
            double avail;
            try {
                super.getValue(metric).getValue();
                avail = Metric.AVAIL_UP;
            } catch (Exception e) {
                avail = Metric.AVAIL_DOWN;
                reportError(metric, e);
            }

            return new MetricValue(avail);
        }

        if (type.equals("FileInfo") ||
            type.equals("DirStats"))
        {
            String attr = metric.getAttributeName();
            double avail;

            try {
                double val =
                    super.getValue(metric).getValue();
                avail = Metric.AVAIL_UP;

                //for directories, verify the type.
                if (attr.equals("Type") &&
                    (val != FileInfo.TYPE_DIR))
                {
                    avail = Metric.AVAIL_DOWN;
                }
            } catch (MetricNotFoundException e) {
                //SigarFileNotFoundException
                avail = Metric.AVAIL_DOWN;
            }

            return new MetricValue(avail);
        }

        if (type.equals("CpuPercList")) {
            try {
                super.getValue(metric);
                // If we can get the metric, return avail up
                return new MetricValue(Metric.AVAIL_UP);
            } catch (Exception e) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
        }

        if (type.equals("MultiProcCpu")) {
            double val = super.getValue(metric).getValue();
            if (val >= 1) {
                return new MetricValue(Metric.AVAIL_UP);
            }
            else {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
        }
        throw new MetricNotFoundException(metric.toString());
    }

    private MetricValue getPhysicalDiskAvail(Metric metric) {
        MetricValue res = new MetricValue(Metric.AVAIL_DOWN);
        String disk = metric.getObjectProperty("disk");
        try {
            List<String> disks = Arrays.asList(Pdh.getInstances("PhysicalDisk"));
            getLog().debug("[getPhysicalDiskAvail] disk='"+disk+"' disks="+disks);
            if(disks.contains(disk)) {
                res = new MetricValue(Metric.AVAIL_UP); 
            }
        } catch (Win32Exception ex) {
            getLog().debug("[getPhysicalDiskAvail] " + ex, ex);
        }
        return res;
    }
    
    private MetricValue getAverageReadWrites() {
        try {
            Sigar sigar = getSigar();
            
            FileSystem[] fslist = sigar.getFileSystemList();
            long totalReadWrites = 0;
            boolean found = false;
            int numOfFs = 0;
            for (int i=0; i<fslist.length; i++) {
                FileSystem fs = fslist[i];                
                if (fs.getType() == FileSystem.TYPE_LOCAL_DISK) {
                    numOfFs++;
                    String dirName = fs.getDirName();
                    FileSystemUsage fsUsage = sigar.getMountedFileSystemUsage(dirName);
                    if (fsUsage != null) {
                        long reads = fsUsage.getDiskReads();
                        if (reads >= 0) {
                            found = true;
                            totalReadWrites += reads;
                        }
                        long writes = fsUsage.getDiskWrites();
                        if (writes >= 0) {
                            totalReadWrites += writes;
                            found = true;
                        }
                    }
                }
            }
            if (found) {
                double averageReadsWrites = ((double)totalReadWrites)/ numOfFs;
                return new MetricValue(averageReadsWrites);
            }
            else {
                return MetricValue.NONE;
            }
        }
        catch(Exception e) {            
            return MetricValue.NONE;
        }
    }

    private double getChildProcessCount(String arg)
        throws MetricNotFoundException {

        try {
            Sigar s = getSigar();
            long processIds[] = s.getProcList();
            ProcessQuery query = ProcessQueryFactory.getInstance().getQuery(arg);
            long parentPid = query.findProcess(s);

            double count = 0;
            for (long pid : processIds) {
                ProcState state;
                try {
                    state = s.getProcState(pid);
                    if (parentPid == state.getPpid()) {
                        count++;
                    }
                } catch (SigarException e) {
                    //ok, Process likely went away
                }
            }
            return count;
        } catch (Exception e) {
            throw new MetricNotFoundException(e.getMessage(), e);
        }
    }

    private double getNumCpus()
        throws MetricNotFoundException {

        try {
            return getSigar().getCpuInfoList().length;
        } catch (Exception e) {
            throw new MetricNotFoundException(e.getMessage(), e);
        }
    }
    
    private long getDiskUsage(String arg)
        throws MetricNotFoundException {

        try {
            return getSigar().getDirUsage(arg).getDiskUsage();
        } catch (Exception e) {
            throw new MetricNotFoundException(e.getMessage(), e);
        }
    }

    @Override
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        ConfigSchema schema = new ConfigSchema();
        if (info.getType() == TypeInfo.TYPE_PLATFORM) {
            if (config.getKeys().contains(TopNConfigurationProperties.ENABLE_TOPN_COLLECTION.getName())
                    || config.getKeys().isEmpty()) {
                for (TopNConfigurationProperties conf : TopNConfigurationProperties.values()) {
                    schema.addOption(conf.getConfigOption());
                }
            }
        }
        return schema;
    }
    
    @Override
    public Collector getNewCollector() {
        getLog().debug("[---------------]" + getTypeInfo().getName());
        if (getPluginData().getPlugin(TYPE_COLLECTOR, getTypeInfo().getName()) == null) {
            getPluginData().addPlugin(TYPE_COLLECTOR, "Win32", SystemPDHCollector.class.getName());
            getPluginData().addPlugin(TYPE_COLLECTOR, "FileServer Physical Disk", SystemPDHCollector.class.getName());
        }
        return super.getNewCollector();
    }
}
