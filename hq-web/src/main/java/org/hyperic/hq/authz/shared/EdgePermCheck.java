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
