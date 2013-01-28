package org.hyperic.hq.api.transfer.impl;

import java.util.List;

import javax.jms.Destination;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.NotificationsTransfer;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.NotificationsMapper;
import org.hyperic.hq.notifications.Q;
import org.hyperic.hq.notifications.UnregisteredException;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.springframework.beans.factory.annotation.Autowired;

public class NotificationsTransferImpl implements NotificationsTransfer {
    protected Destination dest;
    @Autowired
    protected NotificationsMapper mapper;
    @Autowired
    protected Q q;
    @Autowired
    protected ResourceTransfer rscTransfer;
    @Autowired
    protected MeasurementTransfer msmtTransfer;
    
    public NotificationsReport poll() throws UnregisteredException {
        Destination dest = this.dest;
        if (dest==null) {
            throw new UnregisteredException();
        }
        List<? extends BaseNotification> ns = this.q.poll(dest);
        return this.mapper.toNotificationsReport(ns);
    }
    public void unregister() {
        Destination dest = this.dest;
        if (dest!=null) {
            this.dest=null;
            this.q.unregister(dest);
            this.rscTransfer.getEvaluator().unregisterAll(dest);
            this.msmtTransfer.getEvaluator().unregisterAll(dest);
        }
    }
}
