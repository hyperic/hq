/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.dao;

import org.hibernate.Session;
import org.hyperic.hq.appdef.server.session.AppServiceDAO;
import org.hyperic.hq.appdef.server.session.AppSvcDependencyDAO;
import org.hyperic.hq.appdef.server.session.ConfigResponseDAO;
import org.hyperic.hq.appdef.server.session.CpropKeyDAO;
import org.hyperic.hq.appdef.server.session.PlatformDAO;
import org.hyperic.hq.appdef.server.session.ServerDAO;
import org.hyperic.hq.appdef.server.session.ServerTypeDAO;
import org.hyperic.hq.appdef.server.session.ServiceDAO;
import org.hyperic.hq.appdef.server.session.ServiceTypeDAO;
import org.hyperic.hq.appdef.server.session.VirtualDAO;
import org.hyperic.hq.authz.server.session.ResourceDAO;
import org.hyperic.hq.authz.server.session.ResourceGroupDAO;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.server.session.RoleDAO;
import org.hyperic.hq.common.server.session.ConfigPropertyDAO;
import org.hyperic.hq.common.server.session.CrispoDAO;
import org.hyperic.hq.common.server.session.CrispoOptionDAO;
import org.hyperic.hq.dao.AIHistoryDAO;
import org.hyperic.hq.dao.AIIpDAO;
import org.hyperic.hq.dao.AIPlatformDAO;
import org.hyperic.hq.dao.AIScheduleDAO;
import org.hyperic.hq.dao.AIServerDAO;
import org.hyperic.hq.dao.AIServiceDAO;
import org.hyperic.hq.dao.HibernateDAOFactory;
import org.hyperic.hq.events.server.session.ActionDAO;
import org.hyperic.hq.events.server.session.AlertActionLogDAO;
import org.hyperic.hq.events.server.session.AlertConditionLogDAO;
import org.hyperic.hq.events.server.session.AlertDefinitionDAO;
import org.hyperic.hq.events.server.session.TriggerDAO;

public abstract class DAOFactory {
    public abstract Session getCurrentSession();

    public abstract AppServiceDAO       getAppServiceDAO();
    public abstract AppSvcDependencyDAO getAppSvcDepencyDAO();
    public abstract ConfigResponseDAO   getConfigResponseDAO();
    public abstract CpropKeyDAO         getCpropKeyDAO();
    public abstract PlatformDAO         getPlatformDAO();
    public abstract ServerDAO           getServerDAO();
    public abstract ServerTypeDAO       getServerTypeDAO();
    public abstract ServiceDAO          getServiceDAO();
    public abstract ServiceTypeDAO      getServiceTypeDAO();
    public abstract VirtualDAO          getVirtualDAO();

    // Autoinventory DAOs
    public abstract AIPlatformDAO   getAIPlatformDAO();
    public abstract AIServerDAO     getAIServerDAO();
    public abstract AIServiceDAO    getAIServiceDAO();
    public abstract AIIpDAO         getAIIpDAO();
    public abstract AIHistoryDAO    getAIHistoryDAO();
    public abstract AIScheduleDAO   getAIScheduleDAO();

    // Event DAOs
    public abstract ActionDAO            getActionDAO();
    public abstract AlertDefinitionDAO   getAlertDefDAO();
    public abstract TriggerDAO           getTriggerDAO();
    public abstract AlertActionLogDAO    getAlertActionLogDAO();
    public abstract AlertConditionLogDAO getAlertConditionLogDAO();

    // Common DAO
    public abstract ConfigPropertyDAO getConfigPropertyDAO();
    public abstract CrispoDAO         getCrispoDAO();
    public abstract CrispoOptionDAO   getCrispoOptionDAO();

    public abstract ResourceTypeDAO  getResourceTypeDAO();
    public abstract ResourceDAO      getResourceDAO();
    public abstract ResourceGroupDAO getResourceGroupDAO();
    public abstract RoleDAO          getRoleDAO();

    public static DAOFactory getDAOFactory() {
        return HibernateDAOFactory.getInstance();
    }
}
