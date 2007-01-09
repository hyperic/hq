package org.hyperic.hq.dao;

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.common.ConfigProperty;

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

public class ConfigPropertyDAO extends HibernateDAO
{
    public ConfigPropertyDAO(DAOFactory f) {
        super(ConfigProperty.class, f);
    }

    public ConfigProperty findById(Integer id)
    {
        return (ConfigProperty)super.findById(id);
    }

    public void save(ConfigProperty entity)
    {
        super.save(entity);
    }

    public void remove(ConfigProperty entity)
    {
        super.remove(entity);
    }

    public ConfigProperty create(String prefix, String key,
                                 String val, String def)
    {
        ConfigProperty c = new ConfigProperty();
        c.setPrefix(prefix);
        c.setKey(key);
        c.setValue(val);
        c.setDefaultValue(def);
        save(c);
        return c;
    }

    public Collection findByPrefix(String s)
    {
        String sql="from ConfigProperty where prefix=?";
        return getSession().createQuery(sql)
            .setString(0, s)
            .list();
    }
}
