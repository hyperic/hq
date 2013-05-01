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
package org.hyperic.hq.api.transfer;

import org.hyperic.hq.api.model.NotificationsReport;
import org.hyperic.hq.api.model.common.ExternalEndpointStatus;
import org.hyperic.hq.api.model.measurements.HttpEndpointDefinition;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.impl.NotificationsTransferImpl.EndpointStatusAndDefinition;
import org.hyperic.hq.api.transfer.mapping.UnknownEndpointException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.notifications.NotificationEndpoint;
import org.hyperic.hq.notifications.UnregisteredException;
import org.hyperic.hq.notifications.model.InternalResourceDetailsType;

public interface NotificationsTransfer {
    public NotificationsReport poll(String id, ApiMessageContext apiMessageContext) throws UnregisteredException, PermissionException;
    public void unregister(final ApiMessageContext messageContext, String id) throws PermissionException;
    public void register(NotificationEndpoint endpoint, InternalResourceDetailsType type, int authzSubjectId);
    public void register(NotificationEndpoint endpoint, int authzSubjectId);
    public EndpointStatusAndDefinition getEndointStatus(String registrationID) throws PermissionException, NotFoundException;
}
