/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2004-2011], VMware, Inc.
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

package org.hyperic.hq.product.server.session;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.SessionFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.product.Plugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PluginDAO extends HibernateDAO<Plugin> {
    @Autowired
    public PluginDAO(SessionFactory f) {
        super(Plugin.class, f);
    }

    public Plugin create(String name, String version, String path, String md5) {
        Plugin p = new Plugin();
        p.setName(name);
        p.setVersion(version);
        p.setPath(path);
        p.setMD5(md5);
        p.setModifiedTime(System.currentTimeMillis());
        save(p);
        return p;
    }

    public Plugin findByName(String name) {
        String sql = "from Plugin where name=?";
        return (Plugin) getSession().createQuery(sql).setString(0, name).uniqueResult();
    }

    public Plugin getByFilename(String filename) {
        String hql = "from Plugin where path = :filename";
        return (Plugin) getSession().createQuery(hql).setString("filename", filename).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public Collection<Plugin> getPluginsByFileNames(Collection<String> pluginFileNames) {
        if (pluginFileNames.isEmpty()) {
            return Collections.emptyList();
        }
        String hql = "from Plugin where path in (:filenames)";
        return getSession().createQuery(hql).setParameterList("filenames", pluginFileNames).list();
    }

    public long getMaxModTime() {
        final String hql = "select max(modifiedTime) from Plugin";
        final Number num = (Number) getSession().createQuery(hql).uniqueResult();
        if (num == null) {
            // this is a big problem, throw SystemException
            throw new SystemException("cannot fetch max(modifiedTime) from the Plugin table");
        }
        return num.longValue();
    }
    
    protected boolean cacheFindAll() {
        return true;
    }
}
