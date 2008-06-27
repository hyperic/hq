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

package org.hyperic.hq.dao;

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.auth.Principal;

/**
 *
 */
public class PrincipalDAO extends HibernateDAO
{
    public PrincipalDAO(DAOFactory f) {
        super(Principal.class, f);
    }

    public Principal findById(Integer id)
    {
        return (Principal)super.findById(id);
    }

    public void save(Principal entity)
    {
        super.save(entity);
    }

    public void remove(Principal entity)
    {
        super.remove(entity);
    }

    public Principal create(String principal, String passwordHash)
    {
        Principal p = new Principal();

        p.setPrincipal(principal);
        p.setPassword(passwordHash);
        save(p);
        return p;
    }

    public Principal findByUsername(String s)
    {
        String sql = "from Principal where principal=?";
        return (Principal)getSession().createQuery(sql)
            .setString(0, s)
            .uniqueResult();
    }

    public Collection findAllUsers()
    {
        return findAll();
    }
}
