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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.hyperic.util.config.ConfigResponse;

import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfiguration;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.autoinventory.scanimpl.FileScan;

/**
 * Implements the ConfigDriver interface for auto-inventory
 * operations driven interactively via the shell.  Used by the
 * ClientShell_autoinventory command.
 * @see ClientShell_autoinventory#processCommand
 */
public abstract class AutoinventoryShellConfigDriver {

    private ClientShell_autoinventory_subcommand cmd = null;
    private ClientShell shell = null;

    protected ClientShellAuthenticator auth = null;

    public AutoinventoryShellConfigDriver
        (ClientShell_autoinventory_subcommand cmd) {
        this.cmd   = cmd;
        this.shell = cmd.shell;
        this.auth  = cmd.auth;
    }

    public ScanConfigurationCore getScanConfiguration () 
        throws AutoinventoryException {

        ScanConfiguration scanConfig = new ScanConfiguration();

        List scanMethods = new ArrayList();

        //FileScan is the only method we need to config
        //auto-scan does process/registry scan w/o config
        ScanMethod scanMethod = new FileScan();

        ConfigResponse response = configureScanMethod(scanMethod);
        scanConfig.setScanMethodConfig(scanMethod, response);

        List supportedServerSigs = getValidServerSignatures();

        // Which server types do we actually want to scan for
        ServerSignature[] serverSigsToScan = selectServersToScan(supportedServerSigs);

        // Setup the scan config object
        scanConfig.setServerSignatures(serverSigsToScan);

        // _log.debug("Server types=" + StringUtil.listToString(serverTypesToScan));

        return scanConfig.getCore();
    }

    /**
     * Subclasses implement this - given platform basics, tell us
     * what are the valid server signatures.
     * @param pb The basic platform info.
     * @return A List of ServerSignature objects representing the server types
     * that can be found on the specified platform.
     */
    public abstract List getValidServerSignatures() 
        throws AutoinventoryException;

    public Properties getProperties () {
        Properties props = new Properties();
        return props;
    }

    public ConfigResponse configureScanMethod( ScanMethod scanMethod ) 
        throws AutoinventoryException {
        
        ConfigResponse res = null;
        try {
            res = shell.processConfigSchema(scanMethod.getConfigSchema());
            return res;

        } catch ( Exception e ) {
            throw new AutoinventoryException(e);
        }
    }

    public ServerSignature[] selectServersToScan( List serverSigs )
        throws AutoinventoryException {

        PrintStream out = cmd.getOutStream();
        PrintStream err = cmd.getErrStream();
        String response = null;
        boolean parseOK = false;
        StringTokenizer st;
        int stypeNum, i;
        List serverTypesToScan = new ArrayList();
        ServerSignature sig;

        String inputString = "\nSelect server types to scan for "
            + "(enter zero to scan for all types):";
        for ( i=0; i<serverSigs.size(); i++ ) {
            sig = (ServerSignature) serverSigs.get(i);
            inputString += "\n" + String.valueOf(i+1) + ": " 
                + sig.getServerTypeName();
        }

        inputString += "\n> ";
        while (true) {
            try {
                response = shell.getInput(inputString, false);
            } catch ( Exception e ) {
                throw new AutoinventoryException("Could not read input", e);
            }
            if ( response == null || response.trim().length() == 0 ) continue;

            // Handle special case - zero means scan for all server types.
            if ( response.trim().equals("0") ) {
                for ( i=0; i<serverSigs.size(); i++ ) {
                    serverTypesToScan.add(serverSigs.get(i));
                }
                break;
            }

            st = new StringTokenizer(response);
            parseOK = true;
            while ( st.hasMoreTokens() ) {
                try {
                    stypeNum = Integer.parseInt(st.nextToken());
                    serverTypesToScan.add(serverSigs.get(stypeNum-1));
                    
                } catch ( Exception e ) {
                    err.println("Error parsing input: " + e);
                    parseOK = false;
                    break;
                }
            }
            if ( parseOK ) break;
        }

        ServerSignature[] sigArray = new ServerSignature[serverTypesToScan.size()];
        for ( i=0; i<serverTypesToScan.size(); i++ ) {
            sigArray[i] = (ServerSignature) serverTypesToScan.get(i);
        }
        return sigArray;
    }
}
