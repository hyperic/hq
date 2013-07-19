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
package org.hyperic.hq.common.shared;

import java.util.Collection;
import java.util.List;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.AuditImportance;
import org.hyperic.hq.common.server.session.AuditPurpose;

/**
 * Local interface for AuditManager.
 */
public interface AuditManager {
    /**
     * Save an audit and all of it's children.
     */
    public void saveAudit(Audit a);

    /**
     * If there is currently an audit in progress (a container), fetch it.
     */
    public Audit getCurrentAudit();

    /**
     * Delete an audit and all its children.
     */
    public void deleteAudit(Audit a);

    public void popAll();

    /**
     * Pop the audit container off the stack.
     * @param allowEmpty If true, allow the container to pop and be saved with
     *        no children. If the container is empty, and this is true, simply
     *        delete it
     */
    public void popContainer(boolean allowEmpty);

    /**
     * Push a global audit container onto the stack. Any subsequent audits
     * created (via saveAudit) will be added to this container.
     */
    public void pushContainer(Audit newContainer);

    public List<Audit> find(AuthzSubject me, PageInfo pInfo, long startTime, long endTime,
                            AuditImportance minImportance, AuditPurpose purpose, AuthzSubject target, String klazz);

    public Collection<Audit> getOrphanedAudits();

}
