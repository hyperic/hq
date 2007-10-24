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

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.TransactionManagerLocal;
import org.hyperic.hq.common.shared.TransactionManagerUtil;
import org.hyperic.util.Runnee;

/**
 * @ejb:bean name="TransactionManager"
 *      jndi-name="ejb/common/TransactionManager"
 *      local-jndi-name="LocalTransactionManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:util generate="physical"
 * @ejb:transaction type="REQUIRED"
 */
public class TransactionManagerEJBImpl implements SessionBean {

    /**
     * @ejb:interface-method
     */
    public Object runInTransaction(Runnee r) throws Exception {
        return r.run();
    }
    
    public static TransactionManagerLocal getOne() {
        try {
            return TransactionManagerUtil.getLocalHome().create();
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
