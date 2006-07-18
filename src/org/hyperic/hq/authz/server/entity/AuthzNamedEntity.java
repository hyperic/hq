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

package org.hyperic.hq.authz.server.entity;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupLocal;
import org.hyperic.hq.authz.shared.ResourceGroupUtil;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;

public abstract class AuthzNamedEntity extends AuthzEntity {
    /**
     * Name of this operation
     * @ejb:interface-method
     * @ejb:persistent-field
     * @ejb:transaction type="SUPPORTS"
     * @jboss:read-only true
     */
    public abstract String getName();
    /**
     * @ejb:interface-method
     * @ejb:transaction type="MANDATORY"
     */
    public abstract void setName(String name);

    protected void addToAuthzGroup(AuthzSubjectLocal whoami,
                                   ResourceLocal resource)
        throws NamingException, FinderException, PermissionException {
        ResourceGroupLocal authzGroup =
            ResourceGroupUtil.getLocalHome().findByName(AuthzConstants.authzResourceGroupName);
        authzGroup.setWhoami(whoami);
        authzGroup.addResource(resource);
    }
}
