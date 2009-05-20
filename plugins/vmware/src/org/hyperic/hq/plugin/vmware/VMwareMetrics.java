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

package org.hyperic.hq.plugin.vmware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.sigar.vmware.VM;
import org.hyperic.sigar.vmware.VMwareException;
import org.hyperic.sigar.vmware.VMwareServer;

import org.hyperic.hq.product.Metric;

public class VMwareMetrics extends HashMap {
    //XXX need to refactor after moving from MeasurementPlugin to Collector
    private static final Double NO_VALUE = new Double(Double.NaN);

    private static final String CPU_NUMBER = "cpu.number";
    private static final String SYS_CPU_NUMBER = "system." + CPU_NUMBER;

    private static final String[] VM_VARS = {
        "mem.shares", "mem.min", "mem.max", "mem.size", "mem.memctl",
        "mem.swapped", "mem.shared", "mem.active", "mem.overhd"
    };

    private static final String[] VM_CPU_VARS = {
        "syssec", "usedsec", "waitsec" 
    };

    private static final String[] VM_DISK_VARS = {
        "reads", "writes", "KBread", "KBwritten",
    };

    private static final String[] VM_NET_VARS = {
        "totPktsTx", "totPktsRx", "totKBTx", "totKBRx"
    };

    private static final String[] SERVER_CPU_VARS = {
        "idlesec", "usedsec"
    };

    private static final String[] SERVER_VARS = {
        "system.mem.avail", "system.mem.size", "system.mem.active"
    };

    private static final long EXPIRE = (60 * 1000) * 5;

    private static Map cache = new HashMap();

    private long timestamp = 0;

    static String getResource(VM vm, String var)
        throws VMwareException {

        try {
            return vm.getResource(var);
        } catch (VMwareException e) {
            throw new VMwareException(e.getMessage() + ": " + var);
        }
    }

    static String getResource(VMwareServer server, String var)
        throws VMwareException {

        try {
            return server.getResource(var);
        } catch (VMwareException e) {
            throw new VMwareException(e.getMessage() + ": " + var);
        }
    }

    private static void getResourceVars(VM vm, String[] vars, Map metrics)
        throws VMwareException {

        for (int i=0; i<vars.length; i++) {
            String var = vars[i];
            try {
                metrics.put(var, getResource(vm, var));
            } catch (VMwareException e) {
                metrics.put(var, NO_VALUE);
            }
        }
    }

    private static void getCPUVars(VMwareServer server,
                                   String[] vars, Map metrics) {

        final String prefix = "system.cpu.";

        try {
            String cpu = getResource(server, SYS_CPU_NUMBER);
            int num = Integer.parseInt(cpu);

            metrics.put(SYS_CPU_NUMBER, cpu);

            for (int i=0; i<vars.length; i++) {
                double val = 0;

                for (int j=0; j<num; j++) {
                    String ix = prefix + j + "." + vars[i];

                    try {
                        String rval = getResource(server, ix);
                        val += Double.parseDouble(rval);
                    } catch (VMwareException e) {
                    }
                }
                metrics.put(prefix + vars[i], String.valueOf(val));
            }
        } catch (VMwareException e) {
        }
    }

    private static void getCPUVars(VM vm, String[] vars, Map metrics) {
        final String prefix = "cpu.";

        try {
            String cpu = getResource(vm, CPU_NUMBER);
            int num = Integer.parseInt(cpu);

            metrics.put(CPU_NUMBER, cpu);

            for (int i=0; i<vars.length; i++) {
                double val = 0;

                for (int j=0; j<num; j++) {
                    String ix = prefix + j + "." + vars[i];

                    try {
                        String rval = getResource(vm, ix);
                        val += Double.parseDouble(rval);
                    } catch (VMwareException e) {
                    }
                }
                metrics.put(prefix + vars[i], String.valueOf(val));
            }
        } catch (VMwareException e) {
        }
    }

    private static void getDeviceVars(VM vm, String[] vars, Map metrics,
                                      String device)
        throws VMwareException {

        for (int i=0; i<vars.length; i++) {
            String var = device + "." + vars[i];
            try {
                metrics.put(var, getResource(vm, var));
            } catch (VMwareException e) {
                metrics.put(var, NO_VALUE);
            }
        }
    }

    public static Map getInstance(Properties props)
        throws VMwareException {

        synchronized (VMwareConnectParams.LOCK) {
            return getMetrics(props);
        }
    }

    private static Map getMetrics(Properties props)
        throws VMwareException {

        VMwareMetrics metrics = (VMwareMetrics)cache.get(props);
        if (metrics == null) {
            metrics = new VMwareMetrics();
            cache.put(props, metrics);
        }

        long timeNow = System.currentTimeMillis();
        if ((timeNow - metrics.timestamp) < EXPIRE) {
            return metrics;
        }

        VMwareConnectParams params =
            new VMwareConnectParams(props);

        VMwareServer server = new VMwareServer();
        server.connect(params);

        for (int i=0; i<SERVER_VARS.length; i++) {
            String var = SERVER_VARS[i];
            metrics.put(var, getResource(server, var));
        }

        getCPUVars(server, SERVER_CPU_VARS, metrics);

        metrics.timestamp = timeNow;

        server.disconnect();
        server.dispose();

        return metrics;
    }

    public static Map getInstance(Properties props,
                                  String config)
        throws VMwareException {

        synchronized (VMwareConnectParams.LOCK) {
            return getMetrics(props, config);
        }
    }

    private static Map getMetrics(Properties props,
                                  String config)
        throws VMwareException {

        Double up = new Double(Metric.AVAIL_UP);

        VMwareMetrics metrics = (VMwareMetrics)cache.get(config);
        if (metrics == null) {
            metrics = new VMwareMetrics();
            cache.put(config, metrics);
        }

        long timeNow = System.currentTimeMillis();
        if ((timeNow - metrics.timestamp) < EXPIRE) {
            return metrics;
        }

        VMwareConnectParams params =
            new VMwareConnectParams(props);

        VM vm = new VM();
        vm.connect(params, config);

        boolean isOn=false, isESX;
        double avail;
        switch (vm.getExecutionState()) {
            case VM.EXECUTION_STATE_ON:
                isOn = true;
                avail = Metric.AVAIL_UP;
                break;
            case VM.EXECUTION_STATE_OFF:
                avail = Metric.AVAIL_DOWN;
                break;
            case VM.EXECUTION_STATE_STUCK:
                avail = Metric.AVAIL_WARN;
                break;
            case VM.EXECUTION_STATE_SUSPENDED:
                avail = Metric.AVAIL_PAUSED;
                break;
            case VM.EXECUTION_STATE_UNKNOWN:
            default:
                avail = Metric.AVAIL_UNKNOWN;
                break;
        }

        metrics.put("State", new Double(avail));

        isESX =
            vm.getProductInfo(VM.PRODINFO_PRODUCT) == VM.PRODUCT_ESX;

        if (isOn) {
            if (isESX) {
                /* GSX does not support these metrics */
                getResourceVars(vm, VM_VARS, metrics);
                getCPUVars(vm, VM_CPU_VARS, metrics);

                List disks = VMwareDetector.getDisks(vm);
                for (int i=0; i<disks.size(); i++) {
                    String disk = "disk." + disks.get(i);
                    metrics.put(disk + ".avail", up);
                    getDeviceVars(vm, VM_DISK_VARS, metrics, disk);
                }

                List nics = VMwareDetector.getNICs(vm);
                for (int i=0; i<nics.size(); i++) {
                    String nic = "net." + nics.get(i);
                    metrics.put(nic + ".avail", up);
                    getDeviceVars(vm, VM_NET_VARS, metrics, nic);
                }

                metrics.put("Uptime",
                            new Double(vm.getUptime()));
            }
        }

        metrics.timestamp = timeNow;

        vm.disconnect();
        vm.dispose();

        return metrics;
    }
}
