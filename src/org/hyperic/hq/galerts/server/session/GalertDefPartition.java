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

package org.hyperic.hq.galerts.server.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.util.HypericEnum;

/**
 * An alert definition is divided into partitions.  Currently the only two
 * partitions are:
 *
 *  Normal   - Regular style alert definition.  If X happens, el-kabong
 *  Recovery - A partition indicating that an alertable condition is no longer
 *             valid
 */
public abstract class GalertDefPartition 
    extends HypericEnum
{
    private static final String GALERT_FIXED_PROP = "galert.fixed";
    private static final String BUNDLE = "org.hyperic.hq.galerts.Resources"; 
        
    public abstract void execute(Integer id, ExecutionReason execReason);
    
    // This is just a regular alert definition.  
    public static final GalertDefPartition NORMAL = 
        new GalertDefPartition(0, "NORMAL", "galert.partition.normal") 
    {
        public void execute(Integer id, ExecutionReason execReason) {
            GalertManagerLocal gman = GalertManagerEJBImpl.getOne();
            GalertDef def = gman.findById(id);

            GalertManagerEJBImpl.getOne().startEscalation(def, execReason);
        }
    };
    
    // This alert definition executed a recovery alert
    public static final GalertDefPartition RECOVERY = 
        new GalertDefPartition(1, "RECOVERY", "galert.partition.recovery") 
    {
        public void execute(Integer id, ExecutionReason execReason) {
            EscalationManagerLocal eman = EscalationManagerEJBImpl.getOne();
            GalertManagerLocal gman = GalertManagerEJBImpl.getOne();
            AuthzSubjectManagerLocal aman = AuthzSubjectManagerEJBImpl.getOne();
            AuthzSubject overlord = aman.getOverlordPojo();
            
            GalertDef def = gman.findById(id);
            String fixedMessage = ResourceBundle.getBundle(BUNDLE)
                                                .getString(GALERT_FIXED_PROP);
            eman.fixAlert(overlord, def, fixedMessage);
        }
    };
    
    private GalertDefPartition(int code, String desc, String localeProp) {
        super(GalertDefPartition.class, code, desc, localeProp, 
              ResourceBundle.getBundle(BUNDLE)); 
    }
    
    public static GalertDefPartition findByCode(int code) {
        return (GalertDefPartition)
            HypericEnum.findByCode(GalertDefPartition.class, code);
    }
}
