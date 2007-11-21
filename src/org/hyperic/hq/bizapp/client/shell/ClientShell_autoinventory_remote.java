/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_autoinventory_remote extends ShellCommandBase {

    private ClientShell_autoinventory_subcommand owner = null;

    public ClientShell_autoinventory_remote(ClientShell_autoinventory_command ai) {
        owner = ai;
    }

    public void processCommand (String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException {

        String commandName = owner.getCommandName();

        if (args.length != 2) {
            throw new ShellCommandUsageException("Wrong number of arguments");
        }
 
        // Make we can parse the platformid if it's there.
        int platformID = -1;
        AppdefEntityID id = null;
        try {
            id = owner.entityFetcher.getID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                                           args[1]);
            platformID = id.getID();

        } catch (Exception e) {
            throw new ShellCommandUsageException("Invalid platform id: " +
                                                 id.getID());
        }

        if (commandName.endsWith("start")) {
            startRemoteScan(platformID);
        } else if (commandName.endsWith("stop")) {
            stopRemoteScan(platformID);
        } else if (commandName.endsWith("status")) {
            getRemoteStatus(platformID);
        } else {
            throw new ShellCommandUsageException("Unknown command");
        }
    }

    private void startRemoteScan ( int platformID )
        throws ShellCommandExecException {

        // Send the config and platform ID over to start the scan
        ScanConfigurationCore scanConfigCore;
        try {
            AIBoss aiBoss = owner.shell.getBossManager().getAIBoss();
            AppdefBoss appdefBoss = owner.shell.getBossManager().getAppdefBoss(); 
            AutoinventoryShellConfigDriver configDriver
                = new AutoinventoryShellConfigDriver_remote(owner, 
                                                            platformID,
                                                            aiBoss,
                                                            appdefBoss);
            scanConfigCore = configDriver.getScanConfiguration();

            owner.shell.getInput("Press Enter to begin the scan.", false);
            aiBoss.startScan(owner.auth.getAuthToken(), 
                             platformID, 
                             scanConfigCore, 
                             null, // no scanName or
                             null, // scanDec for one time scan
                             null); 

        } catch (Exception e) {
            e.printStackTrace();
            throw new ShellCommandExecException("Couldn't start scan.", e);
        }
    }

    private void stopRemoteScan ( int platformID ) 
        throws ShellCommandExecException {

        // Stop the scan on the platformID
        try {
            AIBoss aiBoss = owner.shell.getBossManager().getAIBoss();
            aiBoss.stopScan(owner.auth.getAuthToken(), platformID);
        } catch (Exception e) {
            throw new ShellCommandExecException("Couldn't stop scan.", e);
        }
    }

    private void getRemoteStatus ( int platformID ) 
        throws ShellCommandExecException {

        // Get the scan status for the platform ID
        ScanState state = null;
        ScanStateCore core = null;
        try {
            AIBoss aiBoss = owner.shell.getBossManager().getAIBoss();
            core = aiBoss.getScanStatus(owner.auth.getAuthToken(), platformID);
            state = new ScanState(core);
        } catch (Exception e) {
            throw new ShellCommandExecException("Couldn't get scan status.", e);
        }

        try {
            state.printFullStatus(getOutStream()); 
       } catch (AutoinventoryException ae) {
            throw new ShellCommandExecException("Error generating status " +
                                                "output.", ae);
        }
    }
}
