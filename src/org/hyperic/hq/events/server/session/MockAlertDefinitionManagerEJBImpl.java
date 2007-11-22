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

package org.hyperic.hq.events.server.session;

import java.util.List;
import java.util.SortedMap;

import javax.ejb.EJBException;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertDefinitionLastFiredUpdateEvent;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.jmock.core.Verifiable;
import org.jmock.expectation.ExpectationList;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * A mock implementation of the alert definition manager EJB.
 */
public class MockAlertDefinitionManagerEJBImpl implements
        AlertDefinitionManagerLocal, Verifiable {
    
    private final ExpectationList alertDefinitionLastFiredTimeEvents = 
                        new ExpectationList("alert def last fired time events");
    
    private volatile Latch alertDefLastFiredTimeUpdateLatch;
    
    
    /**
     * Set the alert def last fired time update events that this EJB can expect 
     * to encounter (in the correct order).
     * 
     * @param events The expected events.
     */
    public void setExpectedAlertDefEventsToUpdate(AlertDefinitionLastFiredUpdateEvent[] events) {
        alertDefinitionLastFiredTimeEvents.addExpectedMany(events);
        alertDefLastFiredTimeUpdateLatch = new Latch();
    }
        
    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#cleanupAlertDefinitions(org.hyperic.hq.appdef.shared.AppdefEntityID)
     */
    public void cleanupAlertDefinitions(AppdefEntityID aeid) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#createAlertDefinition(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.events.shared.AlertDefinitionValue)
     */
    public AlertDefinitionValue createAlertDefinition(AuthzSubjectValue subj,
            AlertDefinitionValue a) throws AlertDefinitionCreateException,
            ActionCreateException, FinderException, PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#deleteAlertDefinitions(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Integer[])
     */
    public void deleteAlertDefinitions(AuthzSubjectValue subj, Integer[] ids)
            throws RemoveException, PermissionException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#deleteAlertDefinitions(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.appdef.shared.AppdefEntityID)
     */
    public void deleteAlertDefinitions(AuthzSubjectValue subj,
            AppdefEntityID aeid) throws RemoveException, PermissionException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#disassociateResource(org.hyperic.hq.appdef.shared.AppdefEntityID)
     */
    public void disassociateResource(AppdefEntityID aeid) {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findAlertDefinitionChildren(java.lang.Integer)
     */
    public PageList findAlertDefinitionChildren(Integer id) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findAlertDefinitionNames(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.Integer)
     */
    public SortedMap findAlertDefinitionNames(AuthzSubjectValue subj,
            AppdefEntityID id, Integer parentId) throws PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findAlertDefinitions(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.events.AlertSeverity, java.lang.Boolean, boolean, org.hyperic.hibernate.PageInfo)
     */
    public List findAlertDefinitions(AuthzSubjectValue subj,
            AlertSeverity minSeverity, Boolean enabled,
            boolean excludeTypeBased, PageInfo info) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findAlertDefinitions(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.appdef.shared.AppdefEntityID, org.hyperic.util.pager.PageControl)
     */
    public PageList findAlertDefinitions(AuthzSubjectValue subj,
            AppdefEntityID id, PageControl pc) throws PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findAlertDefinitions(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.Integer, org.hyperic.util.pager.PageControl)
     */
    public PageList findAlertDefinitions(AuthzSubjectValue subj,
            AppdefEntityID id, Integer parentId, PageControl pc)
            throws PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findAlertDefinitions(org.hyperic.hq.authz.server.session.AuthzSubject, org.hyperic.hq.appdef.shared.AppdefEntityID)
     */
    public List findAlertDefinitions(AuthzSubject subject, AppdefEntityID id)
            throws PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findAllAlertDefinitions(org.hyperic.hq.authz.shared.AuthzSubjectValue)
     */
    public PageList findAllAlertDefinitions(AuthzSubjectValue subj) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findChildAlertDefinitionId(org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.Integer)
     */
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid, Integer pid) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findChildAlertDefinitionId(org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.Integer, boolean)
     */
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid, Integer pid,
            boolean allowStale) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findChildAlertDefinitions(java.lang.Integer)
     */
    public PageList findChildAlertDefinitions(Integer id) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#findTypeBasedDefinitions(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Boolean, org.hyperic.hibernate.PageInfo)
     */
    public List findTypeBasedDefinitions(AuthzSubjectValue subj,
            Boolean enabled, PageInfo info) throws PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getActionsById(java.lang.Integer)
     */
    public ActionValue[] getActionsById(Integer id) throws FinderException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getAppdefEntityIdById(java.lang.Integer)
     */
    public AppdefEntityID getAppdefEntityIdById(Integer id)
            throws FinderException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getById(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Integer)
     */
    public AlertDefinitionValue getById(AuthzSubjectValue subj, Integer id)
            throws FinderException, PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getByIdAndCheck(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Integer)
     */
    public AlertDefinition getByIdAndCheck(AuthzSubjectValue subj, Integer id)
            throws FinderException, PermissionException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getByIdNoCheck(java.lang.Integer, boolean)
     */
    public AlertDefinition getByIdNoCheck(Integer id, boolean refresh)
            throws FinderException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getConditionsById(java.lang.Integer)
     */
    public AlertConditionValue[] getConditionsById(Integer id)
            throws FinderException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getIdFromTrigger(java.lang.Integer)
     */
    public Integer getIdFromTrigger(Integer tid) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#getNameById(java.lang.Integer)
     */
    public String getNameById(Integer id) throws FinderException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#isAlertDefined(org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.Integer)
     */
    public boolean isAlertDefined(AppdefEntityID id, Integer parentId) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#setEscalation(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Integer, java.lang.Integer)
     */
    public void setEscalation(AuthzSubjectValue subj, Integer defId,
            Integer escId) throws PermissionException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#synchAlertDefinitionsLastFiredTimes()
     */
    public void synchAlertDefinitionsLastFiredTimes() {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#updateAlertDefinition(org.hyperic.hq.events.shared.AlertDefinitionValue)
     */
    public AlertDefinitionValue updateAlertDefinition(AlertDefinitionValue adval)
            throws AlertConditionCreateException, ActionCreateException,
            FinderException, RemoveException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#updateAlertDefinitionBasic(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Integer, java.lang.String, java.lang.String, int, boolean)
     */
    public void updateAlertDefinitionBasic(AuthzSubjectValue subj, Integer id,
            String name, String desc, int priority, boolean enabled)
            throws PermissionException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#updateAlertDefinitionEnable(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.events.server.session.AlertDefinition, boolean)
     */
    public void updateAlertDefinitionEnable(AuthzSubjectValue subj,
            AlertDefinition def, boolean enable) throws PermissionException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#updateAlertDefinitionInternalEnable(org.hyperic.hq.authz.shared.AuthzSubjectValue, org.hyperic.hq.events.server.session.AlertDefinition, boolean)
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubjectValue subj,
            AlertDefinition def, boolean enable) throws PermissionException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#updateAlertDefinitionInternalEnable(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Integer, boolean)
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubjectValue subj,
            Integer defId, boolean enable) throws FinderException,
            PermissionException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#updateAlertDefinitionsEnable(org.hyperic.hq.authz.shared.AuthzSubjectValue, java.lang.Integer[], boolean)
     */
    public void updateAlertDefinitionsEnable(AuthzSubjectValue subj,
            Integer[] ids, boolean enable) throws FinderException,
            PermissionException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.hyperic.hq.events.shared.AlertDefinitionManagerLocal#updateAlertDefinitionsLastFiredTimes(org.hyperic.hq.events.AlertDefinitionLastFiredUpdateEvent[])
     */
    public void updateAlertDefinitionsLastFiredTimes(
            AlertDefinitionLastFiredUpdateEvent[] events) {
        alertDefinitionLastFiredTimeEvents.addActualMany(events);
        
        if (alertDefLastFiredTimeUpdateLatch != null) {
            alertDefLastFiredTimeUpdateLatch.release();            
        }
    }

    /**
     * @see javax.ejb.EJBLocalObject#getEJBLocalHome()
     */
    public EJBLocalHome getEJBLocalHome() throws EJBException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see javax.ejb.EJBLocalObject#getPrimaryKey()
     */
    public Object getPrimaryKey() throws EJBException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see javax.ejb.EJBLocalObject#isIdentical(javax.ejb.EJBLocalObject)
     */
    public boolean isIdentical(EJBLocalObject arg0) throws EJBException {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see javax.ejb.EJBLocalObject#remove()
     */
    public void remove() throws RemoveException, EJBException {
        // TODO Auto-generated method stub

    }

    /**
     * @see org.jmock.core.Verifiable#verify()
     */
    public void verify() {
        
        if (alertDefLastFiredTimeUpdateLatch != null) {
            try {
                alertDefLastFiredTimeUpdateLatch.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException("thread interrupted", e);
            }            
        }
        
        alertDefinitionLastFiredTimeEvents.verify();
    }

    public boolean isResourceTypeAlertDefinition(Integer id)
        throws FinderException {
        // TODO Auto-generated method stub
        return false;
    }

}
