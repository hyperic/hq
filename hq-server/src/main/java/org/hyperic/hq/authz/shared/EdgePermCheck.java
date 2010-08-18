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

package org.hyperic.hq.authz.shared;

import java.util.List;

import org.hibernate.Query;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;

public abstract class EdgePermCheck {
    private String _sql;
    private String _subjectParam;
    private String _resourceVar;
    private String _resourceParam;
    private String _distanceParam;
    private String _opsParam;
    
    public EdgePermCheck(String sql, String subjectParam,
                         String resourceVar, String resourceParam,
                         String distanceParam, String opsParam) 
                         
    {
        _sql           = sql;
        _subjectParam  = subjectParam;
        _resourceVar   = resourceVar;
        _resourceParam = resourceParam;
        _distanceParam = distanceParam;
        _opsParam      = opsParam;
    }
    
    public abstract Query addQueryParameters(Query q, AuthzSubject subject,
                                             Resource r,
                                             int distance, List ops);
    
    public String getSql() { return _sql; }
    public String getSubjectParam() { return _subjectParam; }
    public String getResourceVar() { return _resourceVar; }
    public String getResourceParam() { return _resourceParam; }
    public String getDistanceParam() { return _distanceParam; }
    public String getOpsParam() { return _opsParam; }
    
    public String toString() {
        return _sql;
    }
}
