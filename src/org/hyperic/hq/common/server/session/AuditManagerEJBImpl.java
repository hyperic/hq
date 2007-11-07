/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.common.server.session;

import java.util.Iterator;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteCallback;
import org.hyperic.hq.authz.server.session.SubjectRemoveCallback;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.AuditManagerLocal;
import org.hyperic.hq.common.shared.AuditManagerUtil;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditPurpose;
import org.hyperic.hq.common.server.session.Audit;


/**
 * @ejb:bean name="AuditManager"
 *      jndi-name="ejb/common/AuditManager"
 *      local-jndi-name="LocalAuditManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class AuditManagerEJBImpl implements SessionBean {
    private final Log _log = LogFactory.getLog(AuditManagerEJBImpl.class);
    private static final ThreadLocal CONTAINERS = new ThreadLocal();
    
    private final AuditDAO _DAO = new AuditDAO(DAOFactory.getDAOFactory()); 

    /**
     * Save an audit and all of it's children.  
     * 
     * @ejb:interface-method
     */
    public void saveAudit(Audit a) {
        if (a.getStartTime() == 0)
            a.setStartTime(System.currentTimeMillis());
        
        if (getCurrentAudit() != null) {
            getCurrentAudit().addChild(a);
        } else {
            saveRecursively(a);
        }
    }
    
    private void saveRecursively(Audit a) {
        _DAO.save(a);
        for (Iterator i=a.getChildren().iterator(); i.hasNext(); ) {
            Audit child = (Audit)i.next();
            
            saveRecursively(child);
        }
    }

    /**
     * If there is currently an audit in progress (a container), fetch it.
     * 
     * @ejb:interface-method
     */
    public Audit getCurrentAudit() {
        return (Audit)CONTAINERS.get(); 
    }
    
    /**
     * Delete an audit and all its children.
     * 
     * @ejb:interface-method
     */
    public void deleteAudit(Audit a) {
        deleteRecursively(a);
    }
    
    private void deleteRecursively(Audit a) {
        for (Iterator i=a.getChildren().iterator(); i.hasNext(); ) {
            Audit child = (Audit)i.next();
            
            deleteRecursively(child);
        }
        _DAO.remove(a);
    }
    
    /**
     * @ejb:interface-method
     */
    public void popAll() {
        Audit a = getCurrentAudit();
        long now = System.currentTimeMillis();
        
        try {
            while (a != null && a.getParent() != null) {
                if (a.getEndTime() == 0)
                    a.setEndTime(now);
                a = a.getParent();
            }
            
            if (a != null) {
                _log.warn("Unpopped audit container: " + a.getMessage() + 
                          ":  This should be closed manually!");
                if (a.getEndTime() != 0)
                    a.setEndTime(now);
                saveRecursively(a);
            }
        } finally {
            CONTAINERS.set(null);
        }
    }
    
    /**
     * Pop the audit container off the stack.  
     * 
     * @param allowEmpty If true, allow the container to pop and be saved
     *                   with no children.  If the container is empty, and
     *                   this is true, simply delete it
     * @ejb:interface-method
     */
    public void popContainer(boolean allowEmpty) {
        Audit a = getCurrentAudit();
        
        if (a == null) {
            throw new RuntimeException("Expected to pop a container, but had " +
                                       "none");
        }
        
        a.setEndTime(System.currentTimeMillis());
        if (a.getParent() == null) {
            // Root level container.  Save off
            try {
                if (!allowEmpty && a.getChildren().isEmpty()) {
                    deleteRecursively(a);
                } else {
                    saveRecursively(a);
                }
            } finally {
                CONTAINERS.set(null);
            }
        } else {
            CONTAINERS.set(a.getParent());
            if (!allowEmpty && a.getChildren().isEmpty()) {
                a.getParent().removeChild(a);
                deleteRecursively(a);
            }
        }
    }
    
    /**
     * Push a global audit container onto the stack.  Any subsequent audits
     * created (via saveAudit) will be added to this container.
     * 
     * @ejb:interface-method
     */
    public void pushContainer(Audit newContainer) {
        Audit currentContainer = getCurrentAudit();
        
        newContainer.setStartTime(System.currentTimeMillis());
        if (currentContainer == null) {
            HQApp.getInstance().addTransactionListener(new TransactionListener()
            {
                public void beforeCommit() {
                    popAll();
                }

                public void afterCommit(boolean success) {
                }
            });
        } else {
            currentContainer.addChild(newContainer);
        }
        CONTAINERS.set(newContainer);
    }

    /**
     * @ejb:interface-method
     */
    public List find(AuthzSubject me, PageInfo pInfo,
                     long startTime, long endTime, 
                     AuditImportance minImportance, AuditPurpose purpose, 
                     AuthzSubject target, String klazz) 
    {
        return _DAO.find(pInfo, me, startTime, endTime, minImportance, purpose, 
                         target, klazz);
    }
    
    /**
     * @ejb:interface-method
     */
    public void handleResourceDelete(Resource r) {
        _DAO.handleResourceDelete(r);
    }
    
    /**
     * @ejb:interface-method
     */
    public void handleSubjectDelete(AuthzSubject s) {
        _DAO.handleSubjectDelete(s);
    }
    
    private static class ResourceDeleteWatcher 
        implements ResourceDeleteCallback 
    {
        public void preResourceDelete(Resource r) {
            AuditManagerEJBImpl.getOne().handleResourceDelete(r);
        }
    }
    
    private static class SubjectDeleteWatcher 
        implements SubjectRemoveCallback 
    {
        public void subjectRemoved(AuthzSubject toDelete) {
            AuditManagerEJBImpl.getOne().handleSubjectDelete(toDelete);
        }
    }
    
    /**
     * @ejb:interface-method
     */
    public void startup() {
        _log.info("Audit Manager starting up");
        
        HQApp.getInstance()
                .registerCallbackListener(ResourceDeleteCallback.class,
                                          new ResourceDeleteWatcher());

        HQApp.getInstance()
                .registerCallbackListener(SubjectRemoveCallback.class,
                                          new SubjectDeleteWatcher());
    }

    public static AuditManagerLocal getOne() {
        try {
            return AuditManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate() { }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    public void setSessionContext(SessionContext c) {}
}
