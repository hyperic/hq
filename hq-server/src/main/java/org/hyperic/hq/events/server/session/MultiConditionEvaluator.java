/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.events.server.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.TriggerNotFiredEvent;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;



/**
 * Responsible for evaluating a set of alert conditions and firing an
 * {@link AlertConditionsSatisfiedZEvent} if an appropriate logical combination
 * of and/or of conditions has been met within an (optional) time range. This
 * logic was previously encapsulated in a MultiConditionTrigger class. This
 * evaluation is expected to occur after watched events have met a single
 * condition (TriggerFired) or not met a single condition (TriggerNotFired).
 * @author jhickey
 *
 */
public class MultiConditionEvaluator implements AlertConditionEvaluator {

    protected final long timeRange;

    private final Object monitor = new Object();

    private final Map<Integer, AbstractEvent> events;

    /**
     * IDs of the triggers that must be fired for alert conditions to be
     * satisfied (AND)
     */
    private Integer[] andTriggerIds;

    /**
     * IDs of the triggers that can optionally satisfy alert conditions (OR) and
     * their ordering indices within the logical expression
     */
    private final Map orTriggerIds = new HashMap();

    private final Integer alertDefinitionId;

    private final ExecutionStrategy executionStrategy;

    private final Log log = LogFactory.getLog(MultiConditionEvaluator.class);

    /**
     *
     * @param alertDefinitionId The ID of the alert definition whose conditions
     *        are being evaluated
     * @param alertConditions The alerting conditions to evaluate
     * @param timeRange The optional time range all conditions should occur in.
     *        This should be set to 0 (never expire) unless a
     *        CounterExecutionStrategy is being used.
     * @param executionStrategy The {@link ExecutionStrategy} to use for firing
     *        an {@link AlertConditionsSatisfiedZEvent} when all conditions have
     *        been met
     */
    public MultiConditionEvaluator(Integer alertDefinitionId,
                                   Collection alertConditions,
                                   long timeRange,
                                   ExecutionStrategy executionStrategy)
    {
        this.alertDefinitionId = alertDefinitionId;
        this.timeRange = timeRange;
        this.executionStrategy = executionStrategy;
        this.events = new LinkedHashMap<Integer, AbstractEvent>();
        initializeWatchedTriggers(alertConditions);
    }

    /**
     *
     * @param alertDefinitionId The ID of the alert definition whose conditions
     *        are being evaluated
     * @param alertConditions The alerting conditions to evaluate
     * @param timeRange The optional time range all conditions should occur in.
     *        This should be set to 0 (never expire) unless a
     *        CounterExecutionStrategy is being used.
     * @param executionStrategy The {@link ExecutionStrategy} to use for firing
     *        an {@link AlertConditionsSatisfiedZEvent} when all conditions have
     *        been met
     * @param events The map in which events should be stored
     */
    public MultiConditionEvaluator(Integer alertDefinitionId,
                                   Collection alertConditions,
                                   long timeRange,
                                   ExecutionStrategy executionStrategy,
                                   Map<Integer, AbstractEvent> events)
    {
        this.alertDefinitionId = alertDefinitionId;
        this.timeRange = timeRange;
        this.executionStrategy = executionStrategy;
        this.events = events;
        initializeWatchedTriggers(alertConditions);
    }

    private void evictExpiredEvents() {
        if (timeRange > 0) {
        	synchronized (events) {
	            for (Iterator<AbstractEvent> eventIter = events.values().iterator(); eventIter.hasNext();) {
	                // event timestamp + timeRange = expiration date. Remove if less
	                // than System.currentTimeMillis
	                if (isExpired(eventIter.next())) {
	                	eventIter.remove();
	                }
	            }
        	}
        }
    }
    
    /**
     * If there are multiple TriggerFiredEvents for the same metric, make sure
     * all of the TriggerFiredEvents were fired from the same measurement event
     */
    private void evictStaleEvents() {
    	Map<Integer, MeasurementEvent> measMap = getLatestMeasurementEvents();

    	if (!measMap.isEmpty()) {
	    	synchronized (events) {
		        for (Iterator<AbstractEvent> iter = events.values().iterator(); iter.hasNext();) {
		        	AbstractEvent savedEvent = iter.next();
		        	if (savedEvent instanceof TriggerFiredEvent) {
		        		TriggerFiredEvent tfe = (TriggerFiredEvent) savedEvent;
		        		if (isStale(tfe, measMap)) {
		    				if (log.isDebugEnabled()) {
		    					log.debug("evicting stale event: " + savedEvent);
		    				}
		        			iter.remove();
		        		}
		            }
		        }
	    	}
    	}
    }
    
    private boolean isStale(TriggerFiredEvent tfe, Map<Integer, MeasurementEvent> measMap) {
    	boolean stale = false;
		AbstractEvent[] triggeredEvents = tfe.getEvents();
		
		for (AbstractEvent triggeredEvent : triggeredEvents) {
    		if (triggeredEvent instanceof MeasurementEvent) {
    			MeasurementEvent me = (MeasurementEvent) triggeredEvent;
    			Integer measurementId = me.getInstanceId();
    			MeasurementEvent latestMe = measMap.get(measurementId);
    			if ((latestMe != null) && (me.getTimestamp() < latestMe.getTimestamp())) {
    				stale = true;
    				break;
    			}
    		}
    	}
		
    	return stale;
    }
    
    private Map<Integer, MeasurementEvent> getLatestMeasurementEvents() {
    	Map<Integer, MeasurementEvent> measMap = new HashMap<Integer, MeasurementEvent>();

    	for (AbstractEvent savedEvent : events.values()) {
        	if (savedEvent instanceof TriggerFiredEvent) {
        		TriggerFiredEvent tfe = (TriggerFiredEvent) savedEvent;
        		AbstractEvent[] triggeredEvents = tfe.getEvents();
        		
        		for (AbstractEvent triggeredEvent : triggeredEvents) {
            		if (triggeredEvent instanceof MeasurementEvent) {
            			MeasurementEvent me = (MeasurementEvent) triggeredEvent;
            			Integer measurementId = me.getInstanceId();
            			MeasurementEvent savedMe = measMap.get(measurementId);
            			if (savedMe == null) {
            				measMap.put(measurementId, me);
            			} else {
            				if (me.getTimestamp() > savedMe.getTimestamp()) {
            					// replace with the latest measurement event
            					measMap.put(measurementId, me);
            				}
            			}
            		}
            	}
            }
        }
    	
    	return measMap;
    }

    protected Collection evaluate(AbstractEvent event) {
    	if (log.isDebugEnabled()) {
            log.debug("evaluate event: " + event);
        }
        Integer triggerId = event.getInstanceId();
        AbstractEvent previous = events.get(triggerId);
        if ((previous != null) && (previous.getTimestamp() > event.getTimestamp())) {
            // The event we are processing is older than our current state
            return null;
        }
        events.put(triggerId, event);
        return getFulfillingConditions();
    }

    protected boolean fireConditionsSatisfied(Collection fulfilled) {
        events.clear();
        return executionStrategy.conditionsSatisfied(new AlertConditionsSatisfiedZEvent(alertDefinitionId.intValue(),
                                                                                 (TriggerFiredEvent[]) fulfilled.toArray(new TriggerFiredEvent[fulfilled.size()])));
    }

    public Integer getAlertDefinitionId() {
        return alertDefinitionId;
    }

    public ExecutionStrategy getExecutionStrategy() {
        return executionStrategy;
    }

    protected Collection getFulfillingConditions() {
        evictExpiredEvents();
        evictStaleEvents();
        List fulfilled = new ArrayList();
        for (Object element : events.values()) {
            AbstractEvent savedEvent = (AbstractEvent) element;
            if (savedEvent instanceof TriggerFiredEvent) {
                fulfilled.add(savedEvent);
            }

        }
        if (triggeringConditionsFulfilled(fulfilled)) {
            return fulfilled;
        }
        return null;
    }

    public Serializable getState() {
        return null;
    }

    long getTimeRange() {
        return timeRange;
    }

    Set getTriggerIds() {
        Set triggers = new HashSet();
        triggers.addAll(orTriggerIds.keySet());
        triggers.addAll(Arrays.asList(andTriggerIds));
        return triggers;
    }

    public void initialize(Serializable initialState) {
        // No-Op.  We start from scratch.
    }

    private void initializeWatchedTriggers(Collection alertConditions) {
        ArrayList andTrigIds = new ArrayList();
        int i = 0;
        for (Iterator iter = alertConditions.iterator(); iter.hasNext();) {
            AlertCondition condition = (AlertCondition) iter.next();
            Integer triggerId = condition.getTrigger().getId();
            if (condition.isRequired()) {
                andTrigIds.add(triggerId);
                i++;
            } else {
                // Put it in the OR list
                orTriggerIds.put(triggerId, new Integer(i));
            }
        }
        this.andTriggerIds = (Integer[]) andTrigIds.toArray(new Integer[andTrigIds.size()]);
    }

    protected boolean isExpired(AbstractEvent event) {
        long expirationTime = event.getTimestamp() + timeRange;
        if (expirationTime < System.currentTimeMillis()) {
            return true;
        }
        return false;
    }

    public void triggerFired(TriggerFiredEvent event) {
        synchronized (monitor) {
            Collection fulfilled = evaluate(event);
            if (fulfilled != null) {
                fireConditionsSatisfied(fulfilled);
            }
        }
    }

    /**
     * Check if the triggering conditions have been fulfilled, meaning the state
     * should be flushed. Contract: for an encoding of: 1&2|3&4|5&6 the
     * conditions are evaluated left-to-right, with no precedence rule of AND
     * over OR (so it's different from Java precedence rules).
     *
     * @return <code>true</code> if the triggering conditions have been
     *         fulfilled; <code>false</code> if not.
     */
    private boolean triggeringConditionsFulfilled(Collection events) {
        boolean result = true;

        Set fulfilled = new HashSet();
        for (Iterator it = events.iterator(); it.hasNext();) {
            AbstractEvent event = (AbstractEvent) it.next();
            fulfilled.add(event.getInstanceId());
        }

        // Now let's see how well we did
        int orInd = 0;

        for (Iterator i = orTriggerIds.keySet().iterator(); i.hasNext();) {
            Object orId = i.next();
            if (fulfilled.contains(orId)) {
                Integer index = (Integer) orTriggerIds.get(orId);
                if (orInd < index.intValue()) {
                    orInd = index.intValue();
                }
            }
        }

        // Go through the subIds
        for (int i = orInd; (result == true) && (i < andTriggerIds.length); i++) {
            // Did not fulfill yet
            if (!fulfilled.contains(andTriggerIds[i])) {
                result = false;
            }
        }

        return result;
    }

    public void triggerNotFired(TriggerNotFiredEvent event) {
        synchronized (monitor) {
            evaluate(event);
        }
    }

}
