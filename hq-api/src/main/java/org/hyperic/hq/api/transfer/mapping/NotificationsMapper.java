package org.hyperic.hq.api.transfer.mapping;

import java.util.List;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.impl.ResourceTransferImpl;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.CreatedResourceNotification;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.notifications.model.RemovedResourceNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationsMapper {
    @Autowired
    protected ResourceMapper rscMapper;
    @Autowired
    protected MeasurementMapper mtmtMapper;
    @Autowired
    protected ExceptionToErrorCodeMapper errorHandler ;

    public NotificationsReport toNotificationsReport(final AuthzSubject subject, ResourceTransfer resourceTransfer, List<? extends BaseNotification> ns) {
        NotificationsReport res = new NotificationsReport(this.errorHandler);
        if (ns.isEmpty()) {
            return new NotificationsReport();
        }
        for(BaseNotification bn:ns) {
            try {
                // expensive for many notifications, the 'instance of' should be used only in the polling mechanism
                if (bn instanceof MetricNotification) {
                    res.addMetric(this.mtmtMapper.toMetricWithId((MetricNotification)bn));
                } else if (bn instanceof CreatedResourceNotification) {
                    res.addNewResource(this.rscMapper.toResource(subject, resourceTransfer,(CreatedResourceNotification )bn));
                } else if (bn instanceof RemovedResourceNotification) {
                    res.addRemovedResourceID(this.rscMapper.toResource((RemovedResourceNotification) bn));
                }
            } catch (Throwable t) {
                //TODO~ put errors in failed resource/failed metrics
            }
        }
        return res;
    }
}
