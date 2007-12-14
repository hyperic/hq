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

import java.io.PrintStream;
import java.util.Hashtable;

import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_trigger_add extends ShellCommandBase {
    private final String BASELINE_THRESHOLD = "BaselineThreshold";
    private final String BASELINE_THRESHOLD_CLASS = "org.hyperic.hq.bizapp.server.trigger.conditional.MeasurementBaselineTrigger";
    private final String MEASUREMENT_THRESHOLD = "MeasurementThreshold";
    private final String MEASUREMENT_THRESHOLD_CLASS = "org.hyperic.hq.bizapp.server.trigger.conditional.MeasurementThresholdTrigger";
    private final String MULTI_CONDITION = "MultiCondition";
    private final String MULTI_CONDITION_CLASS = "org.hyperic.hq.bizapp.server.trigger.conditional.MultiConditionTrigger";
    private final String COUNTER = "Counter";
    private final String COUNTER_CLASS = "org.hyperic.hq.bizapp.server.trigger.frequency.CounterTrigger";
    private final String AVERT = "Avert";
    private final String AVERT_CLASS = "org.hyperic.hq.bizapp.server.trigger.frequency.AvertTrigger";
    private final String ESCALATE = "Escalate";
    private final String ESCALATE_CLASS = "org.hyperic.hq.bizapp.server.trigger.frequency.EscalateTrigger";

    private ClientShell_trigger      owner = null;
    private ClientShellAuthenticator auth  = null;
    private Hashtable             types = new Hashtable();

    public ClientShell_trigger_add(ClientShell_trigger owner,
                                ClientShellAuthenticator auth) 
    {
        this.owner = owner;
        this.auth  = auth;
        this.types.put(AVERT, AVERT_CLASS);
        this.types.put(COUNTER, COUNTER_CLASS);
        this.types.put(ESCALATE, ESCALATE_CLASS);
        this.types.put(MEASUREMENT_THRESHOLD, MEASUREMENT_THRESHOLD_CLASS);
        this.types.put(BASELINE_THRESHOLD, BASELINE_THRESHOLD_CLASS);
        this.types.put(MULTI_CONDITION, MULTI_CONDITION_CLASS);
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        EventsBoss  eventsBoss;
        int         authToken;
        String      mClass;
        PrintStream out = this.getOutStream();

        if(args.length != 1 || 
           (mClass = (String)this.types.get(args[0])) == null)
        {
            throw new
                ShellCommandUsageException("No valid trigger name specified");
        }
        
        eventsBoss = this.owner.getEventsBoss();
        authToken  = this.auth.getAuthToken();

        try {            
            // First setup the trigger
            ConfigSchema schema = eventsBoss.getRegisteredTriggerConfigSchema(authToken,
                                                                              mClass);
            this.getOutStream().println("[ Configure " + args[0] + " ]");
            ConfigResponse response = ((ClientShell)this.getShell()).processConfigSchema(schema);

            boolean isTrigger = true;
            String prompt = "Is trigger a sub-condition [Y|N]: ";
            while (true) {
                String sc = this.getShell().getInput(prompt);
                isTrigger = sc.equalsIgnoreCase("N");
                if (isTrigger || sc.equalsIgnoreCase("Y"))
                    break;

                prompt = "Please enter 'Y' or 'N': ";
            }

            AlertDefinitionValue adval = null;
            if (isTrigger) {
                ConfigSchema cs = new ConfigSchema();
                cs.addOption(new StringConfigOption("name",
                    "Name of alert definition", "CLI Alert Definition"));

                cs.addOption(new StringConfigOption("desc",
                    "Description of alert definition", null));

               IntegerConfigOption type = new IntegerConfigOption(
                    "type",
                    "Resource Type ([1] Platform [2] Server [3] Service)",
                    new Integer(1));
                type.setMinValue(1);
                type.setMaxValue(3);
                cs.addOption(type);

                cs.addOption(new IntegerConfigOption("id",
                    "Resource ID", null));

                cs.addOption(new StringConfigOption("notify",
                    "Filter notifications [Y|N]", "Y"));

                cs.addOption(new StringConfigOption("control",
                    "Filter control actions [Y|N]", "Y"));

                 ConfigResponse cr =
                    ((ClientShell)this.getShell()).processConfigSchema(cs);


                adval = new AlertDefinitionValue();
                adval.setName(cr.getValue("name"));
                adval.setDescription(cr.getValue("desc"));
                adval.setPriority(1);
                adval.setEnabled(true);
                adval.setActive(true);
                adval.setAppdefType(Integer.parseInt(cr.getValue("type")));
                adval.setAppdefId(Integer.parseInt(cr.getValue("id")));
                adval.setNotifyFiltered(cr.getValue("notify").toUpperCase()
                                          .startsWith("Y"));
                adval.setControlFiltered(cr.getValue("control").toUpperCase()
                                           .startsWith("Y"));
            }

            // Create the trigger
            Integer id = eventsBoss.addRegisteredTrigger(authToken, mClass,
                                                         response, adval);

            this.getOutStream().println();
            if (isTrigger) {
                this.getOutStream().println("Alert definition added with ID: " +
                                            id);
            }
            else {
                this.getOutStream().println("Sub-condition added with ID: " +
                                            id);
            }
        } catch(Exception exc){
            throw new ShellCommandExecException("Error processing request ", 
                                                exc);
        }
    }

    public String getUsageShort() {
        return "Add a new trigger";
    }

    public String getUsageHelp(String[] args) {
        return "trigger add name\n    name: includes " + types.keySet();
    }
}
