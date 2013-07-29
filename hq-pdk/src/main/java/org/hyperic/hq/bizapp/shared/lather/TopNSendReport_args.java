package org.hyperic.hq.bizapp.shared.lather;

import java.io.Serializable;
import java.util.List;

import org.hyperic.hq.plugin.system.TopReport;

public class TopNSendReport_args extends SecureAgentLatherValue {

    public static String TOP_REPORTS = "topReports";


    public List<TopReport> getTopReports() {
        if (null != getObject(TOP_REPORTS)) {
            return (List<TopReport>) getObject(TOP_REPORTS);
        }
        return null;
    }

    public void setTopReports(List<TopReport> topReports) {
        addObject(TOP_REPORTS, (Serializable) topReports);
    }

    @Override
    public String toString() {
        return "TopNSendReport_args [Reports: '" + getTopReports() + "']";
    }

}
