package org.hyperic.hq.bizapp.shared.lather;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.plugin.system.TopReport;

public class TopNSendReport_args extends SecureAgentLatherValue {

    private List<TopReport> topReports = new ArrayList<TopReport>();

    public void addReport(TopReport report) {
        this.topReports.add(report);
    }

    public List<TopReport> getTopReports() {
        return topReports;
    }

    public void setTopReports(List<TopReport> topReports) {
        this.topReports = topReports;
    }

}
