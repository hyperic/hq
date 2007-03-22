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

package org.hyperic.hq.bizapp.client.shell;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.ConditionalTriggerSchema;
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.util.config.*;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_alertdef_create extends ClientShellCommand {
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    
    protected static final String PROP_NAME        = "name";
    protected static final String PROP_DESCRIPTION = "description";
    protected static final String PROP_PRIORITY    = "priority";
    
    private static final String PROP_METRIC_ID     = "id";
    private static final String PROP_COMPARATOR    = "comparator";
    private static final String PROP_THRESHOLD     = "threshold";
    protected static final String PROP_OPTION      = "option";
    
    private static final String PARAM_FORMAT =
        "<" + ClientShellParseUtil.makeResourceBlock(PARAM_VALID_RESOURCE) + ">";
    
    protected static String[] AR_REQ = { "Yes (AND)", "NO  (OR)" };

    private Collection metrics;
    
    public ClientShell_alertdef_create(ClientShell shell) {
        super(shell, PARAM_FORMAT);
    }
    
    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        // if this was present, result has a key of KEY_RESOURCE
        ClientShellParseUtil.bubbleUpResourceBlock(result, blockVals);
    }

    protected ConfigSchema getDefCreateSchema() {
        SchemaBuilder sb = new SchemaBuilder();

        sb.add(PROP_NAME,         "Name", "New Alert");
        sb.add(PROP_DESCRIPTION,  "Description", "", true);
        sb.addEnum(PROP_PRIORITY, "Priority", EventConstants.getPriorities(),
                   EventConstants.getPriority(EventConstants.PRIORITY_MEDIUM));

        return sb.getSchema();
    }

    protected ConfigSchema getCondTypeSchema() {
        SchemaBuilder sb = new SchemaBuilder();
        
        sb.addEnum(PROP_OPTION, "Alert Condition Type",
                   EventConstants.getTypes(),
                   EventConstants.getType(EventConstants.TYPE_CHANGE));
        
        return sb.getSchema();
    }

    private String getMetricAliases(AppdefEntityID id)
        throws ShellCommandExecException {
        
        try {
            metrics = getEntityFetcher().getMetricsForID(id);
            if (metrics.size() == 0)
                return "";
        } catch (Exception exc) {
            throw new ShellCommandExecException(exc);
        }
        
        StringBuffer aliases = new StringBuffer(" [ID]       NAME\n");
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            DerivedMeasurementValue dmv = (DerivedMeasurementValue) it.next();
            aliases.append("[")
                .append(dmv.getId())
                .append("]    ")
                .append(dmv.getTemplate().getAlias())
                .append("\n");
        }
        
        aliases.append("   ");
        return aliases.toString();
    }
    
    /**
     * Create an alertdefinitionvalue object
     * which can then be passed into the backend for saving
     * This alertdef object will not have a specific 
     * resource appdef id associated... use with care
     * @return an unsaved AlertDefinitionValue object
     */
    protected AlertDefinitionValue createAlertDefinitionTempl(
        ConfigResponse resp) throws ShellCommandExecException {
        return createAlertDefinitionTempl(null, resp);
    }
    
    /**
     * Create an alertdefinitionvalue object
     * which can then be passed into the backend for saving
     * @return an unsaved AlertDefinitionValue object
     */
    protected AlertDefinitionValue createAlertDefinitionTempl(
        AppdefEntityID aid,
        ConfigResponse resp) throws ShellCommandExecException {
        AlertDefinitionValue alertdef = new AlertDefinitionValue();

        // The basics
        alertdef.setName(resp.getValue(PROP_NAME));
        alertdef.setDescription(resp.getValue(PROP_DESCRIPTION));

        // Set the appdef entity ID
        if(aid != null) {
            alertdef.setAppdefType(aid.getType());
            alertdef.setAppdefId(aid.getID());
        }
        
        // Set the priority
        int pri = EventConstants.getPriority(resp.getValue(PROP_PRIORITY));
        if (pri < 0) {
            throw new ShellCommandExecException(
                "Invalid priority: " + resp.getValue(PROP_PRIORITY));
        }
        alertdef.setPriority(pri);
        
        // Enable it, of course
        alertdef.setEnabled(true);
        
        return alertdef;
    }

    private AlertConditionValue createCondition(
        int type, ConfigResponse resp, Boolean req, AppdefEntityID aid)
        throws MeasurementNotFoundException, ClientShellAuthenticationException,
               SessionTimeoutException, SessionNotFoundException,
               RemoteException, NamingException {
        AlertConditionValue cond = new AlertConditionValue();
        
        // First, set the type of the condition
        cond.setType(type);
        
        // Set whether or not the condition is required
        cond.setRequired(req.booleanValue());

        // Common response values
        cond.setName(resp.getValue(PROP_NAME));
        cond.setOption(resp.getValue(PROP_OPTION));

        DerivedMeasurementValue dmv = null;
        switch (type) {
            case EventConstants.TYPE_THRESHOLD :
            case EventConstants.TYPE_BASELINE :
                cond.setComparator(resp.getValue(PROP_COMPARATOR));
                cond.setThreshold(
                    Double.parseDouble(resp.getValue(PROP_THRESHOLD)));
            case EventConstants.TYPE_CHANGE :
                cond.setMeasurementId(
                    Integer.parseInt(resp.getValue(PROP_METRIC_ID)));
                
                // Find the metric name
                for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                    dmv = (DerivedMeasurementValue) it.next();
                    if (dmv.getId().intValue() == cond.getMeasurementId()) {
                        cond.setName(dmv.getTemplate().getName());
                        break;
                    }
                }
                break;
            default :
                break;
        }
        return cond;
    }
    
    public void processCommand(ParseResult parseRes)
        throws ShellCommandUsageException, ShellCommandExecException {
        ClientShellEntityFetcher fetcher = getEntityFetcher();
                
        // build a list of resource objects
        List resources = new PageList();
        ConfigResponse createResp = null;
        ConfigResponse typeResp = null;
        try {
            AppdefEntityID eid = (AppdefEntityID) parseRes
                    .getValue(ClientShellParseUtil.KEY_RESOURCE);
            resources.add(fetcher.findResourceByID(eid));

            createResp = getClientShell().processConfigSchema(
                    getDefCreateSchema());

        } catch (EOFException exc) {
            getOutStream().println("\nAlert definition creation aborted");
            return;
        } catch (Exception exc) {
            throw new ShellCommandExecException(exc.getMessage(), exc);
        }

        // create an alert def template without id and type
        AlertDefinitionValue def = this.createAlertDefinitionTempl(createResp);
        
        // next we need to find out the condition set for this alert
        // we'll use the first appdef id in the list to find out the necessary details
        // FIXME this needs to be able to deal with incompatible metric collection 
        // setups. for now, it will ignore resources with incompatible schedules
        AppdefEntityID firstId = ((AppdefResourceValue)resources.get(0)).getEntityId();
        
        ArrayList conditions = new ArrayList();
        
        HashMap typeMap = new HashMap();
        
        // Store all condition 'ConfigResponse' objects in a Map
        // so they can be added by every alert
        // key is config resp, value is Boolean.TRUE or .FALSE
        HashMap reqMap = new HashMap();
        
        //  Create the conditions until user says no-mo
        boolean req = true;
        try {
            // Need to find out what type of alert the user wants
            typeResp = getClientShell()
                    .processConfigSchema(getCondTypeSchema());
        
            int type = EventConstants.getType(typeResp.getValue(PROP_OPTION));
            
            // Get the proper schema from the user
            ConfigResponse condResp = getCondSchemaByType(firstId, type);
            
            // add it to the list
            conditions.add(condResp);
            typeMap.put(condResp, new Integer(type));
            reqMap.put(condResp, Boolean.valueOf(req));
            
            // get the email and user ids for the alertdef from the user
            ConfigSchema emailActionSchema = getEntityFetcher()
                    .getActionConfigSchema(new EmailActionConfig()
                                                   .getImplementor());
            getClientShell().getOutStream()
                    .println("[ Configure Email Actions ]");

            ConfigResponse actionResp = getClientShell()
                    .processConfigSchema(emailActionSchema);
            getClientShell().getOutStream().println();

            // fix for HQ-333: on invalid user input, allow user the opportunity
            // to supply a proper value
            while ( true ) {
                try {
                    if (getEntityFetcher().ensureNamesAreIds(actionResp)) {
                        break;
                    }
                    actionResp = getClientShell()
                            .processConfigSchema(emailActionSchema);
                    getClientShell().getOutStream().println();
                    break;
                } catch (InvalidOptionValueException e) {
                    getClientShell()
                            .getOutStream()
                            .println("An invalid value or a list of values was entered.  Please re-enter.");
                    actionResp = getClientShell()
                            .processConfigSchema(emailActionSchema);
                }
            }

            // save the alert for each resource in the resources list
            for(int i = 0 ; i < resources.size(); i++) {
                AppdefResourceValue aRes =
                    (AppdefResourceValue) resources.get(i);
                saveAlertByResource(aRes, def, conditions, typeMap, reqMap,
                                    actionResp);
            } 
            
        } catch (InvalidOptionException e) {
            throw new ShellCommandExecException(
                "An invalid option was entered.", e);
        } catch (Exception e) {
            throw new ShellCommandExecException(
                "An exception has occured creating alert definition." + e.getMessage(), e);
        }
    }    

    /**
     * @param fetcher
     * @param type
     * @param def
     * @param condList
     * @param req
     */
    protected void saveAlertByResource(AppdefResourceValue aRes,
                                     AlertDefinitionValue def,
									 ArrayList conditions,
									 HashMap typeMap,
                                     HashMap reqMap,
                                     ConfigResponse actionResp) 
        throws MeasurementNotFoundException, 
               ClientShellAuthenticationException, 
               SessionTimeoutException, 
               SessionNotFoundException, 
               RemoteException, 
               NamingException, 
               ShellCommandExecException, 
               ShellCommandUsageException {
        
        // clone the alert definition so we dont store
        // the condition n times
        AlertDefinitionValue clone = new AlertDefinitionValue(def);
        
        AppdefEntityID eid = aRes.getEntityId();
        
        // give the alert def an identity
        clone.setAppdefType(eid.getType());
        clone.setAppdefId(eid.getID());
        
        // Now create a new condition for the alert definition
        clone.removeAllConditions();
        for(Iterator i = conditions.iterator(); i.hasNext();) {
            ConfigResponse aResp = (ConfigResponse)i.next();
            
            Integer type = (Integer) typeMap.get(aResp);
            Boolean required = (Boolean)reqMap.get(aResp);
            
            clone.addCondition(this.createCondition(type.intValue(), 
                                                    aResp, 
                                                    required,
                                                    eid));        
        }
        
        try {
            // now save the alert definition in the backend
            clone = getEntityFetcher().createAlertDefinition(clone);
            getShell().sendToOutStream("Alert Definition '" +
                    def.getName() + "' (ID: " + clone.getId() + 
                    ") has been added to resource '" +
                    aRes.getName() + "'");

        } catch (Exception e) {
            throw new ShellCommandExecException(e);
        }
        
        // finally add the notifications
        clone.removeAllActions();
        addNotifications(clone, actionResp);
    }

    /**
     * @param id
     * @param type
     * @return
     * @throws ShellCommandExecException
     * @throws EncodingException
     * @throws IOException
     */
    protected ConfigResponse getCondSchemaByType(AppdefEntityID id, int type)
        throws ShellCommandExecException, EncodingException, IOException {
        ConfigSchema condSchema =
            ConditionalTriggerSchema.getConfigSchema(type);
        
        ConfigOption entType = (ConfigOption)
            condSchema.getOption(ConditionalTriggerSchema.CFG_TYPE);
        if (entType != null)
            entType.setDefault(String.valueOf(id.getType()));
        
        ConfigOption entId = 
            condSchema.getOption(ConditionalTriggerSchema.CFG_ID);
        if (entId != null)
            entId.setDefault(String.valueOf(id.getID()));

        switch (type) {
        case EventConstants.TYPE_THRESHOLD:
        case EventConstants.TYPE_BASELINE :
        case EventConstants.TYPE_CHANGE:
            // Print out metrics
            getOutStream().println(getMetricAliases(id));
            break;
        default:
            break;
        }

        ConfigResponse condResp =
            getClientShell().processConfigSchema(condSchema);
        return condResp;
    }

    /**
     * @param def
     * @throws ShellCommandUsageException
     * @throws ShellCommandExecException
     */
    private void addNotifications(AlertDefinitionValue def, ConfigResponse actionResp) 
        throws ShellCommandUsageException, ShellCommandExecException {
        
        ClientShell_alertdef_add adder =
            new ClientShell_alertdef_add(getClientShell());
        adder.processCommand(
            new String[] { "-action", "Email",
                           String.valueOf(def.getId()) },
                           actionResp);
    
    }

    public String getSyntaxArgs() {
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
               "> <resource>";
    }

    public String getUsageShort() {
        return "Add an alert definition to a resource";
    }

    public String getUsageHelp(String[] args) {
        return "    "
            + getUsageShort()
            + ".\n" 
            + "\n    The command will prompt for alert definition properties."
            + "\n    Adding measurement related conditions requires the alias of"
            + "\n    the measurements, which will be listed."
            + "\n";   
    }
}
