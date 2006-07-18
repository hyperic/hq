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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.autoinventory.shared.AISchedulePK;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.util.jdbc.IDGeneratorFactory;
import org.hyperic.util.jdbc.SequenceRetrievalException;

/**
 * @ejb:bean name="AISchedule"
 *      jndi-name="ejb/autoinventory/AISchedule"
 *      local-jndi-name="LocalAISchedule"
 *      view-type="local"
 *      type="CMP"
 *      cmp-version="2.x"
 *
 * @ejb:finder signature="org.hyperic.hq.autoinventory.shared.AIScheduleLocal findByScanName(java.lang.String name)"
 *      query="SELECT OBJECT(s) FROM AISchedule AS s WHERE s.scanName = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @ejb:finder signature="org.hyperic.hq.autoinventory.shared.AIScheduleLocal findById(java.lang.Integer id)"
 *      query="SELECT OBJECT(s) FROM AISchedule AS s WHERE s.id = ?1"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @ejb:finder signature="java.util.Collection findByEntityFireTimeDesc(int type, int id)"
 *      query="SELECT OBJECT(s) FROM AISchedule AS s WHERE s.entityType = ?1 AND s.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityFireTimeDesc(int type, int id)"
 *      query="SELECT OBJECT(s) FROM AISchedule AS s WHERE s.entityType = ?1 AND s.entityId = ?2 ORDER BY s.nextFireTime DESC"
 *
 * @ejb:finder signature="java.util.Collection findByEntityFireTimeAsc(int type, int id)"
 *      query="SELECT OBJECT(s) FROM AISchedule AS s WHERE s.entityType = ?1 AND s.entityId = ?2"
 *      unchecked="true"
 *      result-type-mapping="Local"
 * @jboss.query signature="java.util.Collection findByEntityFireTimeAsc(int type, int id)"
 *      query="SELECT OBJECT(s) FROM AISchedule AS s WHERE s.entityType = ?1 AND s.entityId = ?2 ORDER BY s.nextFireTime"
 *
 * @ejb:value-object name="AISchedule" extends="org.hyperic.hq.autoinventory.AIScheduleBaseValue" match="*"
 *
 * @jboss:table-name table-name="EAM_AUTOINV_SCHEDULE"
 * @jboss:create-table false
 * @jboss:remove-table false
 */
public abstract class AIScheduleEJBImpl 
    implements EntityBean {

    public final String ctx = AIScheduleEJBImpl.class.getName();
    public final String SEQUENCE_NAME = "EAM_AUTOINV_SCHEDULE_ID_SEQ";
    public static final String DATASOURCE_NAME = HQConstants.DATASOURCE;
    protected Log log = LogFactory.getLog(ctx);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:pk
     * @ejb:pk-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract Integer getId();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setId(Integer Id);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="ENTITY_TYPE"
     * @jboss:read-only true
     */
    public abstract Integer getEntityType();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setEntityType(Integer type);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:column-name name="ENTITY_ID"
     * @jboss:read-only true
     */
    public abstract Integer getEntityId();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setEntityId(Integer id);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getSubject();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setSubject(String subject);

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public void setScheduleValue(ScheduleValue schedule)
        throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream o = new ObjectOutputStream(os);
        o.writeObject(schedule);
        o.flush();
        os.close();

        setScheduleValueBytes(os.toByteArray());
    }

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     */
    public ScheduleValue getScheduleValue()
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream is = 
            new ByteArrayInputStream(getScheduleValueBytes());
        ObjectInputStream o = new ObjectInputStream(is);
        
        ScheduleValue schedule = (ScheduleValue)o.readObject();
        
        return schedule;
    }

    /**
     * Internal storage of schedule value objects.  Not meant for external
     * use.  Use getScheduleValue() and setScheduleValue instead
     *
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="SCHEDULEVALUEBYTES"
     * @jboss:read-only true
     */
    public abstract byte[] getScheduleValueBytes();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setScheduleValueBytes(byte[] schedule);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getTriggerName();

    /**
     * @ejb:interface-method
     */
    public abstract void setTriggerName(String triggerName);

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract String getJobName();

    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setJobName(String triggerName);

    /**
     * Next fire time.  This data could be stale.  Its up to the
     * the generator of the value objects to refresh this field.
     * 
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:value-object match="*"
     * @jboss:read-only true
     */
    public abstract long getNextFireTime();

    /**
     * @ejb:interface-method
     */
    public abstract void setNextFireTime(long nextFireTime);

    /**
     * @ejb:interface-method
     * @ejb:value-object match="*"
     * @ejb:persistent-field
     * @jboss:column-name name="JOB_ORDER_DATA"
     * @jboss:read-only true
     */
    public abstract String getJobOrderData();

    /**
     * @ejb:interface-method
     * @ejb:persistent-field
     * @jboss:column-name name="JOB_ORDER_DATA"
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setJobOrderData(String jobOrderData);

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
     * @jboss:read-only true
     */
    public abstract AIScheduleValue getAIScheduleValue();

    /**
     * @ejb:interface-method
     */
    public abstract void setAIScheduleValue(AIScheduleValue value);

    /**
     * @ejb:interface-method
     * @ejb:create-method
     */
    public AISchedulePK ejbCreate(AppdefEntityID entityId,
                                  String subject,
                                  String scanName,
                                  String scanDesc,
                                  ScheduleValue schedule,
                                  long nextFire, String triggerName,
                                  String jobName)
        throws CreateException
    {
        try {
            Integer id = 
                new Integer((int)IDGeneratorFactory.getNextId(ctx,
                                                              SEQUENCE_NAME, 
                                                              DATASOURCE_NAME));
            setId(id);
            setEntityId(entityId.getId());
            setEntityType(new Integer(entityId.getType()));
            setSubject(subject);
            setScheduleValue(schedule);
            setNextFireTime(nextFire);
            setTriggerName(triggerName);
            setJobName(jobName);
            setJobOrderData(null);
            setScanName(scanName);
            setScanDesc(scanDesc);
        } catch (Exception e) {
            this.log.error("Unable to create autoinventory history entry");
            throw new CreateException("Failed to create autoinventory history" +
                                      " entry: " + e.getMessage());
        }
            
        return null;
    }

    public void ejbPostCreate(AppdefEntityID entityId,
                              String subject,
                              String scanName,
                              String scanDesc,
                              ScheduleValue schedule,
                              long nextFire, String triggerName,
                              String jobName) {}

    public void ejbActivate() throws RemoteException {}
    public void ejbPassivate() throws RemoteException {}
    public void ejbLoad() throws RemoteException {}
    public void ejbStore() throws RemoteException {}
    public void ejbRemove() throws RemoteException, RemoveException {}
    public void unsetEntityContext() throws RemoteException {}
}
