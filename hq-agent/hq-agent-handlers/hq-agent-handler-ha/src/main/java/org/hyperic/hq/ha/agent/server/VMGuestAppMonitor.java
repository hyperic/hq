package org.hyperic.hq.ha.agent.server;

public class VMGuestAppMonitor {

    public native int enable();

    public native int disable();

    public native int isEnabled();

    public native int markActive();

    public native String getAppStatus();

    static {
        System.loadLibrary("VMGuestAppMonitorNative");
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Usage: VMGuestAppMonitor enable|disable|markActive|isEnabled|getStatus");
            System.exit(1);
        }

        VMGuestAppMonitor appMon = new VMGuestAppMonitor();

        System.out.println("args 0 " + args[0]);
        int result = -1;
        if (args[0].equals("enable")) {
            result = appMon.enable();
        } else if (args[0].equals("disable")) {
            result = appMon.disable();
        } else if (args[0].equals("markActive")) {
            result = appMon.markActive();
        } else if (args[0].equals("isEnabled")) {
            result = appMon.isEnabled();
        } else if (args[0].equals("getStatus")) {
            String status = appMon.getAppStatus();
            System.out.println(args[0] + " status " + status);
            System.exit(0);
        } else {
            System.out.println("Bad command " + args[0]);
            System.exit(1);
        }

        System.out.println(args[0] + " result " + result);
    }
}