package org.hyperic.hq.bizapp.shared.lather;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.plugin.system.TopReport;

public class TopNSendReport_args extends SecureAgentLatherValue {

    public static String topNReports="topReports";


    public List<TopReport> getTopReports() {
        return (List<TopReport>) getObjectList(topNReports)[0];
    }

    public void setTopReports(List<TopReport> topReports) {
        addObjectToList(topNReports, topReports);
    }

}
