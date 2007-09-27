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

package org.hyperic.hq.plugin.weblogic;

/**
 * Control a weblogic application node.
 */
public class WeblogicNodeControl
    extends WeblogicControlPlugin {

    protected static final String DEFAULT_SCRIPT = "startManagedWebLogic.sh";

    private boolean useNodeManager() {
        String program = getControlProgram();
        return (program == null) || (program.length() == 0);
    }

    protected void start() {
        if (useNodeManager()) {

            getLog().debug("start via nodemanager: " + getAdminMetric());

            String output = (String)invokeAction(getAdminMetric(), "synchronousStart");
            if (getResult() != RESULT_SUCCESS) {
                //synchronousStart blocks, so we know we've failed here.
                return;
            }

            if (isWeblogicRunning()) {
                setResult(RESULT_SUCCESS);
            }
            else {
                if (output.length() == 0) {
                    //XXX we should check NodeManager state
                    setMessage("start failed without exception " +
                               "(NodeManager not running?)");
                }
                else {
                    setMessage(output);
                }
                setResult(RESULT_FAILURE);
            }
        }
        else {
            //script
            super.start();
        }
    }

    protected String[] getCommandArgs() {
        return new String[] {
            getConfig().getValue(WeblogicMetric.PROP_SERVER),
            getConfig().getValue(WeblogicMetric.PROP_ADMIN_URL),
        };
    }
}
