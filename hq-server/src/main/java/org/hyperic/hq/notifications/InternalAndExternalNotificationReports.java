package org.hyperic.hq.notifications;

public class InternalAndExternalNotificationReports {
    protected InternalNotificationReport internalReport;
    protected String externalReport;
    
    public InternalAndExternalNotificationReports(InternalNotificationReport internalReport, String externalReport) {
        this.internalReport=internalReport;
        this.externalReport=externalReport;
    }

    public InternalNotificationReport getInternalReport() {
        return internalReport;
    }

    public String getExternalReport() {
        return externalReport;
    }

}
