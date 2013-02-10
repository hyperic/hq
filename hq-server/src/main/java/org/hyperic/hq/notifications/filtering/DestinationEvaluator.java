package org.hyperic.hq.notifications.filtering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.notifications.model.BaseNotification;
import org.hyperic.hq.notifications.model.NotificationGroup;
import org.springframework.stereotype.Component;

@Component
public class DestinationEvaluator {
    private final Log log = LogFactory.getLog(DestinationEvaluator.class);
    // TODO~ change to write through versioning (each node would have versioning - write on one version, read another, then sync between them), o/w will pose problems in scale
    protected Map<Integer,FilterChain<BaseNotification>> regToFilter = new ConcurrentHashMap<Integer,FilterChain<BaseNotification>>();
    protected Map<Class<? extends BaseNotification>,Set<Integer>> entityTypeToReg = new ConcurrentHashMap<Class<? extends BaseNotification>,Set<Integer>>();
    protected Map<Integer,Class<? extends BaseNotification>> regToEntityType = new  ConcurrentHashMap<Integer,Class<? extends BaseNotification>>();
    /**
     * append filters
     * 
     * @param dest
     * @param filters
     */
    public Integer register(Class<? extends BaseNotification> entityType, Collection<? extends Filter<? extends BaseNotification,? extends FilteringCondition<?>>> filters) {
        if (filters==null || filters.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("no filters were passed to be registered");
            }
        }
        Integer regID = createRegID();
        FilterChain<BaseNotification> filterChain = this.regToFilter.get(regID);
        if (filterChain==null) {
            filterChain = new FilterChain<BaseNotification>(filters);
            this.regToFilter.put(regID,filterChain); 
        } else {
            filterChain.addAll(filters);
        }
        Set<Integer> regIDSet = entityTypeToReg.get(entityType);
        if (regIDSet==null) {
            regIDSet = new HashSet<Integer>();
            regIDSet.add(regID);
            entityTypeToReg.put(entityType, regIDSet);
        } else {
            regIDSet.add(regID);
        }
        regToEntityType.put(regID,entityType);
        
        return regID;
    }
    /**
     * unregister all filters assigned to this destination
     * @param dest
     */
    public void unregister(Integer regID) {
        FilterChain<? extends BaseNotification> filterChain = this.regToFilter.remove(regID);
        if (log.isDebugEnabled()) {
            if (filterChain==null) {
                log.debug("no filters were previously registered with registration " + regID);
            } else {
                // TODO~ remove all filter chain filters from it
                log.debug("un-registering all previously regitered filters from registration " + regID + ":\n" + filterChain);
            }
        }
        Class<? extends BaseNotification> entityType = regToEntityType.remove(regID);
        entityTypeToReg.remove(entityType);
//        removeRegistrationFromDB(regID);
    }
    public List<NotificationGroup> evaluate(final List<? extends BaseNotification> entities, Class<? extends BaseNotification> entityType) {
        List<NotificationGroup> nsGrpList = new ArrayList<NotificationGroup>();
        Set<Integer> regIDForEntityTypeSet = this.entityTypeToReg.get(entityType);
        if (regIDForEntityTypeSet==null) {
            return nsGrpList;
        }
        for(Integer regID:regIDForEntityTypeSet) {
            FilterChain<BaseNotification> filterChain = regToFilter.get(regID);
            List<? extends BaseNotification> filteredEntities = null;
            filteredEntities = ((List<? extends BaseNotification>) filterChain.filter(entities));
            if (filteredEntities!=null) {
                NotificationGroup nsGrp = new NotificationGroup(regID, filteredEntities);
                nsGrpList.add(nsGrp);
            }
        }
        return nsGrpList;
    }
    
    //TODO~ replace with DB persisted reg ID
    static Integer regID = 0; 
    private Integer createRegID() {
        return ++regID;
    }
}
