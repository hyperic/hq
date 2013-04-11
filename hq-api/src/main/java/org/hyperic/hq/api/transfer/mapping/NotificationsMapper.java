package org.hyperic.hq.api.transfer.mapping;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.api.model.Notification;
import org.hyperic.hq.api.model.NotificationType;
import org.hyperic.hq.api.model.NotificationsGroup;
import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.common.ExternalEndpointStatus;
import org.hyperic.hq.api.model.measurements.HttpEndpointDefinition;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.notifications.BasePostingStatus;
import org.hyperic.hq.notifications.EndpointQueue;
import org.hyperic.hq.notifications.EndpointStatus;
import org.hyperic.hq.notifications.HttpEndpoint;
import org.hyperic.hq.notifications.RegistrationStatus;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.CreatedResourceNotification;
import org.hyperic.hq.notifications.model.MetricNotification;
import org.hyperic.hq.notifications.model.RemovedResourceNotification;
import org.hyperic.hq.notifications.model.ResourceChangedContentNotification;
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

    public NotificationsReport toNotificationsReport(AuthzSubject subject, String regId,
                                                     ResourceTransfer resourceTransfer,
                                                     ResourceDetailsType resourceDetailsType,
                                                     List<? extends BaseNotification> ns) {
        NotificationsReport res = new NotificationsReport(this.errorHandler, regId);
        if (ns==null || ns.isEmpty()) {
            return new NotificationsReport(null, regId);
        }
        List<Notification> creationNotifications = null;
        List<Notification> updateNotifications = null;
        List<Notification> removalNotifications = null;
        
        for(BaseNotification bn:ns) {
            if (bn instanceof MetricNotification) {
                if (creationNotifications==null) {
                    creationNotifications = new ArrayList<Notification>();
                }
                Notification n = this.mtmtMapper.toMetricWithId((MetricNotification)bn);
                if (n!=null) {
                    creationNotifications.add(n);
                }
            } else if (bn instanceof CreatedResourceNotification) {
                if (creationNotifications==null) {
                    creationNotifications = new ArrayList<Notification>();
                }
                try {
                    Notification n = this.rscMapper.toResource(subject, resourceTransfer, resourceDetailsType,(CreatedResourceNotification )bn);
                    if (n!=null) {
                        creationNotifications.add(n);
                    } 
                } catch (Throwable t) {
                    String resId = String.valueOf(((CreatedResourceNotification) bn).getResourceID());
                    res.addFailedResource(resId, ExceptionToErrorCodeMapper.ErrorCode.RESOURCE_NOT_FOUND_BY_ID.getErrorCode(), null, new Object[] {""});
                }
            } else if (bn instanceof RemovedResourceNotification) {
                if (removalNotifications==null) {
                    removalNotifications = new ArrayList<Notification>();
                }
                Notification n = this.rscMapper.toResource((RemovedResourceNotification) bn);
                if (n!=null) {
                    removalNotifications.add(n);
                }
            } else if (bn instanceof ResourceChangedContentNotification) {
                if (updateNotifications==null) {
                    updateNotifications = new ArrayList<Notification>();
                }
                Notification n = this.rscMapper.toChangedResourceContent(resourceDetailsType, (ResourceChangedContentNotification) bn);
                if (n!=null) {
                    updateNotifications.add(n);
                }
            }
        }
        List<NotificationsGroup> ngList = res.getNotificationsGroupList();
        if (creationNotifications!=null && !creationNotifications.isEmpty()) {
            NotificationsGroup ng = new NotificationsGroup(NotificationType.Create);
            ng.setNotifications(creationNotifications);
            ngList.add(ng);
        }
        if (removalNotifications!=null && !removalNotifications.isEmpty()) {
            NotificationsGroup ng = new NotificationsGroup(NotificationType.Delete);
            ng.setNotifications(removalNotifications);
            ngList.add(ng);
        }
        if (updateNotifications!=null && !updateNotifications.isEmpty()) {
            NotificationsGroup ng = new NotificationsGroup(NotificationType.Update);
            ng.setNotifications(updateNotifications);
            ngList.add(ng);
        }

        return res;
    }

    public HttpEndpointDefinition toHttpEndpoint(final HttpEndpoint backendEndpoint) {
        HttpEndpointDefinition externalEndpoint = new HttpEndpointDefinition();
        externalEndpoint.setUrl(backendEndpoint.getUrl().toString());
        externalEndpoint.setUsername(backendEndpoint.getUsername());
        externalEndpoint.setContentType(backendEndpoint.getContentType());
        externalEndpoint.setEncoding(backendEndpoint.getEncoding());
        return externalEndpoint;
    }

    public ExternalEndpointStatus toEndpointStatus(EndpointQueue.EndpointAndRegStatus endpointAndRegStatus) {
        EndpointStatus endpointStatus = endpointAndRegStatus.getEndpointStatus();
        RegistrationStatus regStat = endpointAndRegStatus.getRegStatus();
        ExternalEndpointStatus externalEndpointStatus = new ExternalEndpointStatus();
        externalEndpointStatus.setCreationTime(regStat.getCreationTime());
        externalEndpointStatus.setStatus(ExternalEndpointStatus.OK);

        if (endpointStatus!=null) {
            BasePostingStatus lastPostStatus = endpointStatus.getLast();
            if (!regStat.isValid()) {
                externalEndpointStatus.setStatus(ExternalEndpointStatus.INVALID);
                // the lastPostStatus must exist and be a failure if the registration status is invalid
                externalEndpointStatus.setMessage(lastPostStatus.getMessage());
            } else if (lastPostStatus!=null && !lastPostStatus.isSuccessful()) {
                externalEndpointStatus.setStatus(ExternalEndpointStatus.ERROR);
                externalEndpointStatus.setMessage(lastPostStatus.getMessage());
            }

            BasePostingStatus lastSuccessful = endpointStatus.getLastSuccessful();
            if (lastSuccessful!=null) {
                externalEndpointStatus.setLastSuccessful(lastSuccessful.getTime());
            }
            
            BasePostingStatus lastFailure = endpointStatus.getLastFailure();
            if (lastFailure!=null) {
                externalEndpointStatus.setLastFailure(lastFailure.getTime());
            }
        }

        return externalEndpointStatus;
    }
}
