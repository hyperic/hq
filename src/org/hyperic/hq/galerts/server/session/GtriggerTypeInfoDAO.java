package org.hyperic.hq.galerts.server.session;

import org.hibernate.criterion.Expression;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class GtriggerTypeInfoDAO 
    extends HibernateDAO
{
    public GtriggerTypeInfoDAO(DAOFactory f) {
        super(GtriggerTypeInfo.class, f);
    }

    public GtriggerTypeInfo findById(Integer id) {
        return (GtriggerTypeInfo)super.findById(id);
    }

    void save(GtriggerTypeInfo tInfo) {
        super.save(tInfo);
    }

    void remove(GtriggerTypeInfo tInfo) {
        super.remove(tInfo);
    }
    
    public GtriggerTypeInfo find(GtriggerType type) {
        Class typeClass = type.getClass();
        
        return (GtriggerTypeInfo)
            getSession().createCriteria(GtriggerTypeInfo.class)
                        .add(Expression.eq("typeClass", typeClass))
                        .uniqueResult();
    }
}
