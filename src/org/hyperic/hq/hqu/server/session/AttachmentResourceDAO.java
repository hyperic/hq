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

package org.hyperic.hq.hqu.server.session;

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.dao.HibernateDAO;

class AttachmentResourceDAO
    extends HibernateDAO
{
    AttachmentResourceDAO(DAOFactory f) {
        super(AttachmentResource.class, f);
    }

    private boolean resourceIsPrototype(ResourceType rt) {
        String name = rt.getName();
        
        return name.equals(AuthzConstants.platformPrototypeTypeName) ||
            name.equals(AuthzConstants.serverPrototypeTypeName) ||
            name.equals(AuthzConstants.servicePrototypeTypeName);
    }
    
    Collection findFor(Resource r, ViewResourceCategory cat) {
        if (resourceIsPrototype(r.getResourceType())) {
            String sql = "select a from AttachmentResource a " +
                "join a.resource r " +
                "where r = :resource and " +
                "a.categoryEnum = :cat";
            
            return getSession()
                .createQuery(sql)
                .setParameter("resource", r)
                .setParameter("cat", cat.getDescription())
                .list();
        }
        
        String sql = "select a from AttachmentResource a " +
            "where (a.resource = :resource or a.resource = :proto) and " +
            "a.categoryEnum = :cat";
        
        return getSession()
            .createQuery(sql)
            .setParameter("resource", r)
            .setParameter("proto", r.getPrototype())
            .setParameter("cat", cat.getDescription())
            .list();
    }
}
