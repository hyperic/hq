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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.util.shell.ShellCommandExecException;

public class AutoinventoryShellConfigDriver_remote 
    extends AutoinventoryShellConfigDriver {

    private int authToken;
    private int platformID;
    private AIBoss aiBoss;
    private AppdefBoss appdefBoss;

    public AutoinventoryShellConfigDriver_remote
        ( ClientShell_autoinventory_subcommand cmd,
          int platformID,
          AIBoss aiBoss,
          AppdefBoss appdefBoss) {

        super(cmd);
        this.platformID = platformID;
        this.aiBoss = aiBoss; 
        this.appdefBoss = appdefBoss;
    }

    public List getValidServerSignatures() 
        throws AutoinventoryException {

        List results = new ArrayList();

        try {
            PlatformTypeValue ptype = getPlatformType(this.platformID);
            ServerTypeValue[] serverTypes = ptype.getServerTypeValues();

            List stypes = Arrays.asList(serverTypes);
            Map serversigs = aiBoss.getServerSignatures(auth.getAuthToken(),
                                                        stypes);
            Iterator i = serversigs.keySet().iterator();
            while (i.hasNext()) {
                //XXX: fix root cause
                ServerSignature sig = (ServerSignature)serversigs.get(i.next());
                if (sig.getServerTypeName() == null) {
                    continue;
                }
                results.add(sig);
            }
            return results;

        } catch ( Exception e ) {
            throw new AutoinventoryException("Unexpected exception: " + e, e);
        }
    }
    
    private PlatformTypeValue getPlatformType(int platformID)
        throws ShellCommandExecException {
        try { 
            PlatformValue pValue =
                this.appdefBoss.findPlatformById(auth.getAuthToken(),
                                                 new Integer(platformID));
            return pValue.getPlatformType();
        } catch ( Exception e ) {
            throw new ShellCommandExecException("Could not find platform type for id=" +
                                                platformID);
        }
    }
}
