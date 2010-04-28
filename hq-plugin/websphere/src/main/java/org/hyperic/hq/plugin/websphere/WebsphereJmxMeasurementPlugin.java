package org.hyperic.hq.plugin.websphere;

public class WebsphereJmxMeasurementPlugin
    extends WebsphereMeasurementPlugin {

    public boolean useJMX() {
        return WebsphereProductPlugin.useJMX;
    }
}
