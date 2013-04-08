/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2013], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.api.transfer.impl;

import java.io.StringWriter;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.common.ExternalEndpointStatus;
import org.hyperic.hq.api.model.common.ExternalRegistrationStatus;
import org.hyperic.hq.api.model.measurements.HttpEndpointDefinition;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.MeasurementTransfer;
import org.hyperic.hq.api.transfer.NotificationsTransfer;
import org.hyperic.hq.api.transfer.ResourceTransfer;
import org.hyperic.hq.api.transfer.mapping.ExceptionToErrorCodeMapper;
import org.hyperic.hq.api.transfer.mapping.NotificationsMapper;
import org.hyperic.hq.api.transfer.mapping.UnknownEndpointException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.notifications.EndpointQueue;
import org.hyperic.hq.notifications.HttpEndpoint;
import org.hyperic.hq.notifications.InternalNotificationReport;
import org.hyperic.hq.notifications.NotificationEndpoint;
import org.hyperic.hq.notifications.EndpointStatus;
import org.hyperic.hq.notifications.RegistrationStatus;
import org.hyperic.hq.notifications.UnregisteredException;
import org.hyperic.hq.notifications.filtering.FilterChain;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;
import org.hyperic.util.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.hyperic.hq.notifications.RegistrationStatus;

@Component("notificationsTransfer")
public class NotificationsTransferImpl implements NotificationsTransfer {
    private static final Log log = LogFactory.getLog(NotificationsTransferImpl.class);

    @Autowired
    private NotificationsMapper mapper;
    @Autowired
    private EndpointQueue endpointQueue;
    @Autowired
    private ResourceTransfer rscTransfer;
    @Autowired
    private MeasurementTransfer msmtTransfer;
    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    // will be replaced by the destinations the invokers of this API will pass when registering

    @Transactional (readOnly=true)
    public NotificationsReport poll(long registrationId, Integer subjectId) throws UnregisteredException {
        final InternalNotificationReport report = endpointQueue.poll(registrationId);
        return getNotificationReport(subjectId, report);
    }

    private NotificationsReport getNotificationReport(Integer subjectId, InternalNotificationReport report) {
        AuthzSubject subject = authzSubjectManager.getSubjectById(subjectId);
        InternalResourceDetailsType internalResourceDetailsType = report.getResourceDetailsType();
        ResourceDetailsType resourceDetailsType = null;
        resourceDetailsType = ResourceDetailsType.valueOf(internalResourceDetailsType);
        return mapper.toNotificationsReport(subject, rscTransfer, resourceDetailsType, report.getNotifications());
    }

    @Transactional (readOnly=true)
    public NotificationsReport poll(long registrationId, ApiMessageContext apiMessageContext)
    throws UnregisteredException {
        AuthzSubject subject = apiMessageContext.getAuthzSubject();
        return poll(registrationId, subject.getId());
    }

    @Transactional (readOnly=false)
    public void unregister(long registrationId) {
        NotificationEndpoint endpoint = endpointQueue.unregister(registrationId);
        rscTransfer.unregister(endpoint);
        msmtTransfer.unregister(endpoint);
    }

    public void register(NotificationEndpoint endpoint, final int authzSubjectId) {
        register(endpoint, null, authzSubjectId);
    }

    public void register(NotificationEndpoint endpoint, InternalResourceDetailsType type, final int authzSubjectId) {
        final Transformer<InternalNotificationReport, String> t = new Transformer<InternalNotificationReport, String>() {
            @Override
            public String transform(InternalNotificationReport internalReport) {
                try {
                    final NotificationsReport report = getNotificationReport(authzSubjectId, internalReport);
	                final JAXBContext context = JAXBContext.newInstance(NotificationsReport.class);
	                final StringWriter writer = new StringWriter();
	                context.createMarshaller().marshal(report, writer);
	                return writer.toString();
                } catch (JAXBException e) {
                    throw new SystemException(e);
                }
            }
        };
        endpointQueue.register(endpoint, type, t);
    }

    public EndpointStatusAndDefinition getEndointStatus(long registrationID) {
        NotificationEndpoint backendEndpoint = this.endpointQueue.getEndpoint((long) registrationID);
        if (backendEndpoint==null) {
            ExternalEndpointStatus externalEndpointStatus = new ExternalEndpointStatus();
            externalEndpointStatus.setStatus(ExternalEndpointStatus.INVALID);
            externalEndpointStatus.setMessage("registration " + registrationID + " does not exist");
            return new EndpointStatusAndDefinition(null,externalEndpointStatus);
        }
        if (!(backendEndpoint instanceof HttpEndpoint)) {
            ExternalEndpointStatus externalEndpointStatus = new ExternalEndpointStatus();
            externalEndpointStatus.setStatus(ExternalEndpointStatus.INVALID);
            externalEndpointStatus.setMessage("registration " + registrationID + " exists but is not registered to an endpoint");
            return new EndpointStatusAndDefinition(null,externalEndpointStatus);
        }
        HttpEndpointDefinition endpoint = new HttpEndpointDefinition();
        this.mapper.toHttpEndpoint((HttpEndpoint) backendEndpoint,endpoint);
        RegistrationStatus regStat = new RegistrationStatus();
        EndpointStatus endpointStatus = new EndpointStatus();
        this.endpointQueue.getEndpointAndRegStatus(registrationID,endpointStatus,regStat);
        ExternalEndpointStatus externalEndpointStatus = new ExternalEndpointStatus();
        this.mapper.toEndpointStatus(endpointStatus,externalEndpointStatus, regStat);
        return new EndpointStatusAndDefinition(endpoint,externalEndpointStatus);
    }
    
    public static class EndpointStatusAndDefinition {
        protected HttpEndpointDefinition endpoint;
        protected ExternalEndpointStatus externalEndpointStatus;
        
        public EndpointStatusAndDefinition(HttpEndpointDefinition endpoint,
                ExternalEndpointStatus externalEndpointStatus) {
            this.endpoint = endpoint;
            this.externalEndpointStatus = externalEndpointStatus;
        }
        public HttpEndpointDefinition getEndpoint() {
            return endpoint;
        }
        public ExternalEndpointStatus getExternalEndpointStatus() {
            return externalEndpointStatus;
        }
    }
}
