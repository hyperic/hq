package org.hyperic.hq.galerts.server.session;

import org.hibernate.criterion.Expression;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class ExecutionStrategyTypeInfoDAO 
    extends HibernateDAO
{
    public ExecutionStrategyTypeInfoDAO(DAOFactory f) {
        super(ExecutionStrategyTypeInfo.class, f);
    }

    public ExecutionStrategyTypeInfo findById(Integer id) {
        return (ExecutionStrategyTypeInfo)super.findById(id);
    }

    void save(ExecutionStrategyTypeInfo tInfo) {
        super.save(tInfo);
    }
    
    void save(ExecutionStrategyInfo info) {
        super.save(info);
    }

    void remove(ExecutionStrategyTypeInfo tInfo) {
        super.remove(tInfo);
    }
    
    void remove(ExecutionStrategyInfo info) {
        super.remove(info);
    }
    
    public ExecutionStrategyTypeInfo find(ExecutionStrategyType sType) {
        Class strategyClass = sType.getClass();
        
        return (ExecutionStrategyTypeInfo)
            getSession().createCriteria(ExecutionStrategyTypeInfo.class)
                        .add(Expression.eq("typeClass", strategyClass))
                        .uniqueResult();
    }
}
