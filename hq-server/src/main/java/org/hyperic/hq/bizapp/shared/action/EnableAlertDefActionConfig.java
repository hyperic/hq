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

package org.hyperic.hq.bizapp.shared.action;

import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.InvalidActionDataException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.IntegerConfigOption;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * Shared configs for enable alert definition action
 */
public class EnableAlertDefActionConfig implements ActionConfigInterface {
    public static final String CFG_ID  = "id";

    /** Holds value of property alertDefId. */
    private Integer alertDefId;
    
    private String implementor =
        "com.hyperic.hq.bizapp.server.action.alert.EnableAlertDefAction";

    /** Creates a new instance of SharedEmailAction */
    public EnableAlertDefActionConfig() {
    }
       
    public ConfigSchema getConfigSchema() {
        IntegerConfigOption id;
        ConfigSchema res = new ConfigSchema();

        final int MIN_ID = 10001;
        
        // Determine the ID of the alert definition
        id = new IntegerConfigOption(
            CFG_ID, "Alert Definition ID", new Integer(MIN_ID));
        id.setMinValue(MIN_ID);
        res.addOption(id);

        return res;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.events.ext.ActionInterface#getConfigResponse()
     */
    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse response = new ConfigResponse();
        response.setValue(CFG_ID, String.valueOf(this.getAlertDefId()));
        return response;
    }

    /** Initialize the action instance
     * @param props configuration properties
     *
     */
    public void init(ConfigResponse config) throws InvalidActionDataException {
        // First, let's set the type
        String sId = config.getValue(CFG_ID);
        alertDefId = Integer.valueOf(sId);
    }
    
    /**
     * Returns the alertDefId.
     * @return int
     */
    public int getAlertDefId() {
        return alertDefId.intValue();
    }

    /**
     * Sets the alertDefId.
     * @param alertDefId The type to alertDefId
     */
    public void setAlertDefId(int alertDefId) {
        this.alertDefId = new Integer(alertDefId);
    }

    /**
     * Get the name of the action class
     * @return the name of the implementing class
     */
    public String getImplementor() {
        return implementor;
    }

    public void setImplementor(String implementor) {
        this.implementor = implementor; 
    }
}
