/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
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

import org.hyperic.hq.api.model.Resource;
import org.hyperic.hq.api.model.ResourceDetailsType;
import org.hyperic.hq.api.model.ResourceStatusType;
import org.hyperic.hq.api.model.ResourceType;
import org.hyperic.hq.api.model.Resources;
import org.hyperic.hq.api.model.resources.RegisteredResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceBatchResponse;
import org.hyperic.hq.api.model.resources.ResourceFilterRequest;
import org.hyperic.hq.api.services.impl.ApiMessageContext;
import org.hyperic.hq.api.transfer.impl.ResourceTransferImpl.Context;
import org.hyperic.hq.api.transfer.mapping.ResourceMapper;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.ObjectNotFoundException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.util.config.EncodingException;

public interface ResourceTransfer {

	Resource getResource(final ApiMessageContext messageContext, final String platformNaturalID, final ResourceType resourceType, 
			final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws SessionNotFoundException, SessionTimeoutException, ObjectNotFoundException ; 
	
	Resource getResource(final ApiMessageContext messageContext, final String platformID, final ResourceStatusType resourceStatusType, final int hierarchyDepth, final ResourceDetailsType[] responseMetadata) throws ObjectNotFoundException ; 
	
	RegisteredResourceBatchResponse getResources(final ApiMessageContext messageContext, final ResourceDetailsType responseMetadata, final int hierarchyDepth, final boolean register,final ResourceFilterRequest resourceFilterRequest) throws PermissionException, NotFoundException;
	
	ResourceBatchResponse approveResource(final ApiMessageContext messageContext, final Resources aiResources) ;
	ResourceBatchResponse updateResources(final ApiMessageContext messageContext, final Resources resources);

    void unregister();

    PlatformManager getPlatformManager();

    ResourceManager getResourceManager();

    ResourceMapper getResourceMapper();

    Object initResourceConfig(Context flowContext) throws ConfigFetchException, EncodingException, PluginNotFoundException, PluginException, PermissionException, AppdefEntityNotFoundException;

    String getResourceUrl(ApiMessageContext apiMessageContext, String resourceID);
}//EOI 
