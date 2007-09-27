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

import org.hyperic.util.StringUtil;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_metric_compact extends ClientShellCommand {
    private static final String PARAM_FORMAT = "";

    public ClientShell_metric_compact(ClientShell shell){
        super(shell, PARAM_FORMAT);
    }

    public void processCommand(ParseResult parseRes)
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        try {
            this.getOutStream().println("Begin data compaction...");
            this.getEntityFetcher().compactMeasurementData();
            this.getOutStream().println("...end data compaction");
        } catch (Exception e) {
            throw new ShellCommandExecException(e);
        }
    }

    public String getSyntaxArgs(){
        return "";
    }

    public String getUsageShort(){
        return "Compact metric data in database";
    }

    public String getUsageHelp(String[] args) {
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + " immediately.\n" +               "    This command purges data for removed metrics and " +               "condenses metric data\n    according to administrator settings.";
    }
}
