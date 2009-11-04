/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.events.server.mbean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.server.session.AlertConditionEvaluatorRepository;
import org.hyperic.hq.events.server.session.AlertConditionEvaluatorRepositoryImpl;
import org.hyperic.hq.events.server.session.RecoveryConditionEvaluator;
import org.hyperic.util.StringUtil;

/**
 * Alert Condition Evaluator Diagnostic service
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=AlertConditionEvaluatorDiagnostic"
 */
public class AlertConditionEvaluatorDiagnosticService 
    implements AlertConditionEvaluatorDiagnosticServiceMBean, DiagnosticObject
{

    private Log log = LogFactory.getLog(AlertConditionEvaluatorDiagnosticService.class);
    private AlertConditionEvaluatorRepository alertConditionEvaluatorRepository = null;

    public AlertConditionEvaluatorDiagnosticService() {
        //
    }
    
    void setAlertConditionEvaluatorRepository(AlertConditionEvaluatorRepository repository) {
        this.alertConditionEvaluatorRepository = repository;
    }
    
    private AlertConditionEvaluatorRepository getAlertConditionEvaluatorRepository() {
        return (this.alertConditionEvaluatorRepository == null) 
                    ? AlertConditionEvaluatorRepositoryImpl.getInstance()
                    : this.alertConditionEvaluatorRepository;
    }
    
    public String getStatus() {
        AlertConditionEvaluatorRepository repository = getAlertConditionEvaluatorRepository();
        
        StringBuffer res = new StringBuffer();
        Map alertConditionEvaluators = repository.getAlertConditionEvaluators();
        Map stats = new HashMap();
        
        if (log.isDebugEnabled()) {
            // this will give a list of all the alert condition evaluators
            // and the associated alert definition id
            log.debug("alertConditionEvaluators size=" + alertConditionEvaluators.size());
            log.debug("alertConditionEvaluators=" + alertConditionEvaluators);
        }

        for (Iterator iterator = alertConditionEvaluators.values().iterator(); iterator.hasNext();) {
            AlertConditionEvaluator alertConditionEvaluator = (AlertConditionEvaluator) iterator.next();
            
            String key = alertConditionEvaluator.getClass().getName();
            AlertConditionEvaluatorStats val = null;
            
            if (stats.containsKey(key)) {
                val = (AlertConditionEvaluatorStats) stats.get(key);
            } else {
                val = new AlertConditionEvaluatorStats();
            }
            val.evaluatorCount++;
            if (alertConditionEvaluator.getState() != null) {
                val.evaluatorStateCount++;
            } else if (alertConditionEvaluator instanceof RecoveryConditionEvaluator){
                RecoveryConditionEvaluator rce = (RecoveryConditionEvaluator) alertConditionEvaluator;
                if (rce.getLastAlertFired() != null) {
                    val.evaluatorStateCount++;
                }
            }
            if (alertConditionEvaluator.getExecutionStrategy().getState() != null) {
                val.strategyStateCount++;
            }
            
            stats.put(key, val);            
        }

        res.append("<table border='1'>");
        res.append("<tr><th>Alert Condition Evaluator Class</th>");
        res.append("<th>Total Count</th>");
        res.append("<th>With Evaluator State</th>");
        res.append("<th>With Strategy State</th></tr>");

        AlertConditionEvaluatorStats total = new AlertConditionEvaluatorStats();
        
        for (Iterator iterator = stats.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            AlertConditionEvaluatorStats val = (AlertConditionEvaluatorStats) stats.get(key);
            
            total.evaluatorCount += val.evaluatorCount;
            total.evaluatorStateCount += val.evaluatorStateCount;
            total.strategyStateCount += val.strategyStateCount;
            
            res.append("<tr><td>" + key + "</td>");
            res.append("<td>" + val.evaluatorCount + "</td>");
            res.append("<td>" + val.evaluatorStateCount + "</td>");
            res.append("<td>" + val.strategyStateCount + "</td></tr>");
        }

        res.append("<tr><td>Total</td>");
        res.append("<td>" + total.evaluatorCount + "</td>");
        res.append("<td>" + total.evaluatorStateCount + "</td>");
        res.append("<td>" + total.strategyStateCount + "</td></tr>");
        res.append("</table>");
        
        return res.toString();
    }
    
    /**
     * @jmx:managed-operation
     */
    public String displaySummary() {
        return getStatus();
    }
    
    /**
     * @param alertDefinitionIds A comma-separated list of alert definition ids to inspect 
     * 
     * @jmx:managed-operation
     */
    public String inspectByAlertDefinitionIds(String alertDefinitionIds) {        
        StringBuffer res = new StringBuffer();
        List idList = StringUtil.explode(alertDefinitionIds, ",");
        AlertConditionEvaluatorRepository repository = getAlertConditionEvaluatorRepository();
                
        res.append("<table border='1'>");
        res.append("<tr><th>Alert Definition ID</th>");
        res.append("<th>Alert Condition Evaluator Class</th>");
        res.append("<th>Alert Condition Evaluator State</th>");
        res.append("<th>Execution Strategy State</th></tr>");
        
        for (Iterator iterator = idList.iterator(); iterator.hasNext();) {
            String id = (String) iterator.next();
            id = id.trim();
            
            AlertConditionEvaluator alertConditionEvaluator = 
                repository.getAlertConditionEvaluatorById(new Integer(id));
            String evaluatorClassName = null;
            Serializable alertConditionEvaluatorState = null;
            Serializable executionStrategyState = null;
                        
            if (alertConditionEvaluator != null) {
                evaluatorClassName = alertConditionEvaluator.getClass().getName();
                executionStrategyState = alertConditionEvaluator.getExecutionStrategy().getState();

                if (alertConditionEvaluator instanceof RecoveryConditionEvaluator) {
                    RecoveryConditionEvaluator rce = (RecoveryConditionEvaluator) alertConditionEvaluator;
                    alertConditionEvaluatorState = rce.getLastAlertFired();                    
                } else {
                    alertConditionEvaluatorState = alertConditionEvaluator.getState();
                }
            }
        
            res.append("<tr><td>" + id + "</td>");
            res.append("<td>" + evaluatorClassName + "</td>");
            res.append("<td>" + alertConditionEvaluatorState + "</td>");            
            res.append("<td>" +  executionStrategyState + "</td></tr>");
        }

        res.append("</table>");
        
        return res.toString();
    }
    
    public String getShortName() {
        return "alert diagnostic";
    }
    
    public String getName() {
        return "Alert Condition Evaluator Diagnostic Service";
    }
    
    private class AlertConditionEvaluatorStats {
        protected int evaluatorCount = 0;
        protected int evaluatorStateCount = 0;
        protected int strategyStateCount = 0;
    }
}
