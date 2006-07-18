/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.autoinventory.server.entity;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.shared.AIHistoryPK;
import org.hyperic.hq.autoinventory.shared.AIHistoryValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.IDGeneratorFactory;
import org.hyperic.util.jdbc.IDGeneratorFactory;

/**
 * @ejb:bean name="AIHistory"
 *      jndi-name="ejb/autoinventory/AIHistory"
 *      local-jndi-name="LocalAIHistory"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:value-object name="AIHistory" match="*"
 *
 * @jboss:table-name table-name="EAM_AUTOINV_HISTORY"
 * @jboss:create-table false
 * @jboss:remove-table false
 *
 * @ejb:finder signature="java.util.Collection findByEntity(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 *
 * @ejb:finder signature="java.util.Collection findByEntityStartTimeDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityStartTimeDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.startTime DESC"
 *
 * @ejb:finder signature="java.util.Collection findByEntityStartTimeAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityStartTimeAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.startTime"
 * 
 * @ejb:finder signature="java.util.Collection findByEntityStatusDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityStatusDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.status DESC"
 *
 * @ejb:finder signature="java.util.Collection findByEntityStatusAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityStatusAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.status"
 *
 * @ejb:finder signature="java.util.Collection findByEntityDurationDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityDurationDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.duration DESC"
 *
 * @ejb:finder signature="java.util.Collection findByEntityDurationAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityDurationAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.duration"
 *
 * @ejb:finder signature="java.util.Collection findByEntityDateScheduledDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityDateScheduledDesc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.dateScheduled DESC"
 *
 * @ejb:finder signature="java.util.Collection findByEntityDateScheduledAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityDateScheduledAsc(int type, int id)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.entityType = ?1 AND h.entityId = ?2 ORDER BY h.dateScheduled"
 *
 * @ejb:finder signature="java.util.Collection findByGroupStartTimeDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupStartTimeDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.startTime DESC"
 *
 * @ejb:finder signature="java.util.Collection findByGroupStartTimeAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupid = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupStartTimeAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.startTime"
 *
 * @ejb:finder signature="java.util.Collection findByGroupStatusDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupStatusDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.status DESC"
 *
 * @ejb:finder signature="java.util.Collection findByGroupStatusAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupStatusAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.status"
 *
 * @ejb:finder signature="java.util.Collection findByGroupDurationDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.gropuId = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupDurationDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.duration DESC"
 *
 * @ejb:finder signature="java.util.Collection findByGroupDurationAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupDurationAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.duration"
 *
 * @ejb:finder signature="java.util.Collection findByGroupDateScheduledDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupDateScheduledDesc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.dateScheduled DESC"
 *
 * @ejb:finder signature="java.util.Collection findByGroupDateScheduledAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByGroupDateScheduledAsc(int groupId, int batchId)"
 *      query="SELECT OBJECT(h) FROM AIHistory AS h WHERE h.groupId = ?1 AND h.batchId = ?2 ORDER BY h.dateScheduled"
 */

public abstract class AIHistoryEJBImpl 
    implements EntityBean {

    public final String ctx = AIHistoryEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_AUTOINV_HISTORY_ID_SEQ";
    public static final String DATASOURCE_NAME = HQConstants.DATASOURCE;
    protected Log log = LogFactory.getLog(ctx);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract Integer getId();

    /**
     * @ejb:interface-method
     */
    public abstract void setId(Integer Id);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:column-name name="GROUP_ID"
     * @jboss:read-only true
     */
    public abstract Integer getGroupId();

    /**
     * @ejb:interface-method
     */
    public abstract void setGroupId(Integer groupId);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:column-name name="BATCH_ID"
     * @jboss:read-only true
     */
    public abstract Integer getBatchId();

    /**
     * @ejb:interface-method
     */
    public abstract void setBatchId(Integer batchId);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:column-name name="ENTITY_TYPE"
     * @jboss:read-only true
     */
    public abstract Integer getEntityType();

    /**
     * @ejb:interface-method
     */
    public abstract void setEntityType(Integer type);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:column-name name="ENTITY_ID"
     * @jboss:read-only true
     */
    public abstract Integer getEntityId();

    /**
     * @ejb:interface-method
     */
    public abstract void setEntityId(Integer id);

    /**
     * Method for gettting the appdef entity name based on the type and id.
     * We could just store the name on object creation, but that allows the
     * data to become stale.  This gets the name on the fly.
     * 
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public String getEntityName()
    {
        AppdefEntityValue aVal;
        
        try {
            AuthzSubjectManagerLocal local = 
                AuthzSubjectManagerUtil.getLocalHome().create();
            AuthzSubjectValue subject = local.getOverlord();

            AppdefEntityID id = new AppdefEntityID(getEntityType().intValue(),
                                                   getEntityId().intValue());
            aVal = new AppdefEntityValue(id, subject);
            return aVal.getName();

        } catch (Exception e) {
            // One of NamingException, FinderException, CreateException.
            // Should never happen
        }

        // Not reached
        return "";
    }

    public void setEntityName(String name) {}

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="REQUIRED"
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getSubject();

    /**
     * @ejb:interface-method
     */
    public abstract void setSubject(String subject);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract Boolean getScheduled();

    /**
     * @ejb:interface-method
     */
    public abstract void setScheduled(Boolean scheduled);
    
    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:column-name name="DATE_SCHEDULED"
     * @jboss:read-only true
     */
    public abstract long getDateScheduled();

    /**
     * @ejb:interface-method
     */
    public abstract void setDateScheduled(long dateScheduled);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract long getStartTime();

    /**
     * @ejb:interface-method
     */
    public abstract void setStartTime(long startTime);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract long getEndTime();

    /**
     * @ejb:interface-method
     */
    public abstract void setEndTime(long endTime);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract long getDuration();

    /**
     * @ejb:interface-method
     */
    public abstract void setDuration(long duration);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract String getMessage();

    /**
     * @ejb:interface-method
     */
    public abstract void setMessage(String message);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract String getDescription();

    /**
     * @ejb:interface-method
     */
    public abstract void setDescription(String description);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @ejb:transaction type="REQUIRED"
     * @jboss:read-only true
     */
    public abstract String getStatus();

    /**
     * @ejb:interface-method
     */
    public abstract void setStatus(String status);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getScanName();

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract void setScanName(String name);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getScanDesc();

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract void setScanDesc(String desc);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract byte[] getConfig();

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract void setConfig(byte[] core);

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public ScanConfigurationCore getConfigObj() throws AutoinventoryException {
        return ScanConfigurationCore.deserialize(getConfig());
    }

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public void setConfigObj(ScanConfigurationCore core) 
        throws AutoinventoryException {

        setConfig(core.serialize());
    }

    /**
     * @ejb:interface-method
     * @jboss:read-only true
     */
    public abstract AIHistoryValue getAIHistoryValue();

    /**
     * @ejb:interface-method
     */
    public abstract void setAIHistoryValue(AIHistoryValue value);

    /**
     * @ejb:interface-method
     * @ejb:create-method
     */
    public AIHistoryPK ejbCreate(AppdefEntityID entityId,
                                 Integer groupId,
                                 Integer batchId,
                                 String subject,
                                 ScanConfigurationCore config,
                                 String scanName,
                                 String scanDesc,
                                 Boolean scheduled,
                                 long startTime, long endTime,
                                 long dateScheduled,
                                 String status,
                                 String description,
                                 String message)
        throws CreateException, AutoinventoryException
    {
        try {
            Integer id = 
                new Integer((int)IDGeneratorFactory.getNextId(ctx,
                                                              SEQUENCE_NAME, 
                                                              DATASOURCE_NAME));
            setId(id);
            setGroupId(groupId);
            setBatchId(batchId);
            setEntityId(entityId.getId());
            setEntityType(new Integer(entityId.getType()));
            setSubject(subject);
            setScheduled(scheduled);
            setStartTime(startTime);
            setEndTime(endTime);
            setDateScheduled(dateScheduled);
            setDuration(endTime - startTime);
            setStatus(status);
            setDescription(description);
            if (message != null) {
                setMessage(message);
            }
            setConfigObj(config);
            setScanName(scanName);
            setScanDesc(scanDesc);
        } catch (Exception e) {
            throw new CreateException("Failed to create history " +
                                      "entry: " + e.getMessage());
        }

        return null;
    }

    public void ejbPostCreate(AppdefEntityID entityId,
                              Integer groupId,
                              Integer batchId,
                              String subject,
                              ScanConfigurationCore config,
                              String scanName,
                              String scanDesc,
                              Boolean scheduled,
                              long startTime, long endTime,
                              long dateScheduled,
                              String status,
                              String description,
                              String message) {}

    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}
    public void unsetEntityContext() throws RemoteException {}
}
