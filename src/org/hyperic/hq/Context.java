package org.hyperic.hq;

import org.hyperic.hq.dao.HibernateDAO;

import java.util.HashMap;
import java.util.List;
import java.io.Serializable;

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

public abstract class Context implements Serializable {
    private static final Object HIBERNATE_DAO = new Object();
    private static final Object HIBERNATE_LIST = new Object();

    HashMap map = new HashMap();

    public Object get(Object key) {
        return map.get(key);
    }

    public Object set(Object key, Object value) {
        return map.put(key, value);
    }

    public Object remove(Object key) {
        return map.remove(key);
    }

    public HibernateDAO getContextDao() {
        return (HibernateDAO)map.get(HIBERNATE_DAO);
    }

    public HibernateDAO setContextDao(HibernateDAO dao) {
        return (HibernateDAO)map.put(HIBERNATE_DAO, dao);
    }

    public HibernateDAO removeContextDao() {
        return (HibernateDAO)map.remove(HIBERNATE_DAO);
    }

    public List getQueryResult() {
        return (List)map.get(HIBERNATE_LIST);
    }

    public List setQueryResult(List result) {
        return (List)map.put(HIBERNATE_LIST, result);
    }

    public List removeQueryResult() {
        return (List)map.remove(HIBERNATE_LIST);
    }

}
