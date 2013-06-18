package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.hq.plugin.system.TopData;

public class TopNSendReport_args extends SecureAgentLatherValue {
    private TopData data;

    public TopData getData() {
        return data;
    }

    public void setData(TopData data) {
        this.data = data;
    }

}
