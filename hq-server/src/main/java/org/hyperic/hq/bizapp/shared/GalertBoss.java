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
package org.hyperic.hq.bizapp.shared;

import java.util.List;

import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GtriggerType;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Local interface for GalertBoss.
 */
public interface GalertBoss {

   public ExecutionStrategyTypeInfo registerExecutionStrategy( int sessionId,ExecutionStrategyType stratType ) throws PermissionException, SessionException;

   public ExecutionStrategyTypeInfo findStrategyType( int sessionId,ExecutionStrategyType type ) throws PermissionException, SessionException;

   public GtriggerTypeInfo findTriggerType( int sessionId,GtriggerType type ) throws SessionException;

   public GtriggerTypeInfo registerTriggerType( int sessionId,GtriggerType type ) throws SessionException;

   public ExecutionStrategyInfo addPartition( int sessionId,GalertDef def,GalertDefPartition partition,ExecutionStrategyTypeInfo stratType,ConfigResponse stratConfig ) throws SessionException;

   public GalertDef createAlertDef( int sessionId,String name,String description,AlertSeverity severity,boolean enabled,ResourceGroup group ) throws SessionException;

   public void configureTriggers( int sessionId,GalertDef def,GalertDefPartition partition,List<GtriggerTypeInfo> triggerInfos, List<ConfigResponse> configs ) throws SessionException;

   /**
    * Find all the group alert definitions for a given appdef group.
    * @return a collection of {@link AlertDefinitionBean}s
    * @throws PermissionException
    */
   public PageList<GalertDef> findDefinitions( int sessionId,Integer gid,PageControl pc ) throws SessionException, PermissionException;

   public void markDefsDeleted( int sessionId,GalertDef def ) throws SessionException;

   public void markDefsDeleted( int sessionId,Integer[] defIds ) throws SessionException;

   public GalertDef findDefinition( int sessionId,Integer id ) throws SessionException;

   public Escalatable findEscalatableAlert( int sessionId,Integer id ) throws SessionException, PermissionException;

   public void update( int sessionId,GalertDef def,String name,String desc,AlertSeverity severity,Boolean enabled ) throws SessionException;

   public void update( int sessionId,GalertDef def,Escalation escalation ) throws SessionException;

   /**
    * Bulk enable or disable GalertDefs
    * @throws SessionException if user session cannot be authenticated
    */
   public void enable( int sessionId,GalertDef[] defs,boolean enable ) throws SessionException;

   /**
    * Count the total number of galerts in the time frame
    */
   public int countAlertLogs( int sessionId,Integer gid,long begin,long end ) throws SessionTimeoutException, SessionNotFoundException, PermissionException;

   /**
    * retrieve all escalation policy names as a Array of JSONObject. Escalation json finders begin with json* to be consistent with DAO finder convention
    */
   public JSONObject findAlertLogs( int sessionId,Integer gid,long begin,long end,PageControl pc ) throws JSONException, SessionTimeoutException, SessionNotFoundException, PermissionException;

   /**
    * Get the last fix if available
    */
    public String getLastFix(int sessionID, GalertDef def) throws SessionNotFoundException, SessionTimeoutException;

}
