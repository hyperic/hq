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

package org.hyperic.hq.common.server.session;

import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteRequestedEvent;
import org.hyperic.hq.authz.server.session.SubjectDeleteRequestedEvent;
import org.hyperic.hq.common.shared.AuditManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 *
 */
@Service
@Transactional
public class AuditManagerImpl implements AuditManager, ApplicationListener<ApplicationEvent> {
    private final Log log = LogFactory.getLog(AuditManagerImpl.class);
    private static final ThreadLocal<Audit> CONTAINERS = new ThreadLocal<Audit>();

    private AuditDAO auditDao;

    @Autowired
    public AuditManagerImpl(AuditDAO auditDao) {
        this.auditDao = auditDao;
    }

    /**
     * Save an audit and all of it's children.
     * 
     * 
     */
    public void saveAudit(Audit a) {
        if (a.getStartTime() == 0) {
            a.setStartTime(System.currentTimeMillis());
        }

        if (getCurrentAudit() != null) {
            getCurrentAudit().addChild(a);
        } else {
            saveRecursively(a);
        }
    }

    private void saveRecursively(Audit a) {
        auditDao.save(a);

        if (log.isDebugEnabled()) {
            log.debug("Audit: " + a);
        }

        for (Audit child : a.getChildren()) {
            saveRecursively(child);
        }
    }

    /**
     * If there is currently an audit in progress (a container), fetch it.
     * 
     * 
     */
    @Transactional(readOnly = true)
    public Audit getCurrentAudit() {
        return CONTAINERS.get();
    }

    /**
     * Delete an audit and all its children.
     * 
     * 
     */
    public void deleteAudit(Audit a) {
        deleteRecursively(a);
    }

    private void deleteRecursively(Audit a) {
        for (Audit child : a.getChildren()) {
            deleteRecursively(child);
        }
        auditDao.remove(a);
    }

    /**
     * 
     */
    public void popAll() {
        Audit a = getCurrentAudit();
        long now = System.currentTimeMillis();

        try {
            while (a != null && a.getParent() != null) {
                if (a.getEndTime() == 0) {
                    a.setEndTime(now);
                }
                a = a.getParent();
            }

            if (a != null) {
                log.warn("Unpopped audit container: " + a.getMessage() +
                         ":  This should be closed manually!");
                if (a.getEndTime() != 0) {
                    a.setEndTime(now);
                }
                saveRecursively(a);
            }
        } finally {
            CONTAINERS.set(null);
        }
    }

    /**
     * Pop the audit container off the stack.
     * 
     * @param allowEmpty If true, allow the container to pop and be saved with
     *        no children. If the container is empty, and this is true, simply
     *        delete it
     * 
     */
    public void popContainer(boolean allowEmpty) {
        Audit a = getCurrentAudit();

        if (a == null) {
            throw new RuntimeException("Expected to pop a container, but had " + "none");
        }

        a.setEndTime(System.currentTimeMillis());
        if (a.getParent() == null) {
            // Root level container. Save off
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
     * Push a global audit container onto the stack. Any subsequent audits
     * created (via saveAudit) will be added to this container.
     * 
     * 
     */
    public void pushContainer(Audit newContainer) {
        Audit currentContainer = getCurrentAudit();

        newContainer.setStartTime(System.currentTimeMillis());
        if (currentContainer == null) {
            TransactionSynchronizationManager
                .registerSynchronization(new TransactionSynchronization() {
                    public void suspend() {
                    }

                    public void resume() {
                    }

                    public void flush() {
                    }

                    public void beforeCompletion() {
                    }

                    public void beforeCommit(boolean readOnly) {
                        popAll();
                    }

                    public void afterCompletion(int status) {
                    }

                    public void afterCommit() {
                    }
                });
        } else {
            currentContainer.addChild(newContainer);
        }
        CONTAINERS.set(newContainer);
    }

    /**
     * 
     */
    @Transactional(readOnly = true)
    public List<Audit> find(AuthzSubject me, PageInfo pInfo, long startTime, long endTime,
                            AuditImportance minImportance, AuditPurpose purpose,
                            AuthzSubject target, String klazz) {
        return auditDao.find(pInfo, me, startTime, endTime, minImportance, purpose, target, klazz);
    }

    private void handleResourceDelete(Resource r) {
        auditDao.handleResourceDelete(r);
    }

    private void handleSubjectDelete(AuthzSubject s) {
        auditDao.handleSubjectDelete(s);
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ResourceDeleteRequestedEvent) {
            handleResourceDelete(((ResourceDeleteRequestedEvent) event).getResource());
        } else if (event instanceof SubjectDeleteRequestedEvent) {
            handleSubjectDelete(((SubjectDeleteRequestedEvent) event).getSubject());
        }
    }

    public Collection<Audit> getOrphanedAudits() {
        return auditDao.getOrphanedAudits();
    }

}
