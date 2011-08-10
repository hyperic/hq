/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.product.shared;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ServiceType;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

/**
 * Local interface for ProductManager.
 */
public interface ProductManager {

    public TypeInfo getTypeInfo(AppdefEntityValue value) throws PermissionException, AppdefEntityNotFoundException;

    public PluginManager getPluginManager(String type) throws PluginException;

    public String getMonitoringHelp(AppdefEntityValue entityVal, Map<?, ?> props) throws PluginNotFoundException,
        PermissionException, AppdefEntityNotFoundException;

    public ConfigSchema getConfigSchema(String type, String name, AppdefEntityValue entityVal,
                                        ConfigResponse baseResponse) throws PluginException,
        AppdefEntityNotFoundException, PermissionException;

    public void deploymentNotify(String pluginName, File dir)
    throws PluginNotFoundException, VetoException, NotFoundException;

    public void updateDynamicServiceTypePlugin(String pluginName, Set<ServiceType> serviceTypes)
        throws PluginNotFoundException, VetoException, NotFoundException;

}
