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
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.paramParser.BlockHandler;
import org.hyperic.util.paramParser.DateParser;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParamParser;
import org.hyperic.util.paramParser.ParseException;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_alert_delete extends ShellCommandBase
    implements BlockHandler {
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_GROUP,
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    
    private static final String PARAM_FROM     = "-from";
    private static final String PARAM_TO       = "-to";

    private static final String BLOCK_FROM     = "FromBlock";
    private static final String BLOCK_TO       = "ToBlock";

    private static final String RANGE_BLOCK =
        "$" + BLOCK_FROM + " " + PARAM_FROM + " #PastDate " +
        " [$" + BLOCK_TO + " " + PARAM_TO + " #PastDate] ";

    private static final String PARAM_FMT =
        "<" + ClientShellParseUtil.makeResourceBlock(PARAM_VALID_RESOURCE) + 
        "> | <" + RANGE_BLOCK + ">";

    private ClientShell              shell;
    private ClientShellEntityFetcher entityFetcher;
    private ParamParser              paramParser;

    public ClientShell_alert_delete(ClientShell shell){
        this.shell         = shell;
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                         shell.getAuthenticator());
        ClientShellParserRetriever retriever =
            new ClientShellParserRetriever(this.entityFetcher);
        this.paramParser = new ParamParser(PARAM_FMT, retriever, this);
    }

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        AppdefEntityID entity;
        ParseResult parseRes;
        Long from, to;

        try {
            parseRes = this.paramParser.parseParams(args);
        } catch(ParseException exc){
            if(exc.getExceptionOfType(ApplicationException.class) != null||
               exc.getExceptionOfType(SystemException.class) != null)
            {
                throw new ShellCommandExecException(exc);
            }

            throw new ShellCommandUsageException(this.getUsageShort());
        }

        entity = (AppdefEntityID)
            parseRes.getValue(ClientShellParseUtil.KEY_RESOURCE);

        from   = (Long)parseRes.getValue(BLOCK_FROM);
        to     = (Long)parseRes.getValue(BLOCK_TO);
        
        int count;
        if (entity != null) {
            count = deleteResourceAlerts(entity);
        } else if (from != null) {
            count = deleteAlertsInTimeRange(from, to);
        } else {
            throw new ShellCommandUsageException(this.getUsageHelp(null));
        }
        
        this.getOutStream().println("Total of " + count + " alerts deleted");
    }

    private int deleteResourceAlerts(AppdefEntityID aeid)
        throws ShellCommandExecException 
    {
        try {
            return this.entityFetcher.deleteResourceAlerts(aeid);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    private int deleteAlertsInTimeRange(Long begin, Long end)
        throws ShellCommandExecException 
    {
        try {
            long endMillis;
            if (end == null)
                endMillis = System.currentTimeMillis();
            else
                endMillis = end.longValue();
            return this.entityFetcher.deleteAlertsInTimeRange(begin.longValue(),
                                                              endMillis);
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }
    
    public String getSyntaxArgs(){
        return "< resource | timerange >";
    }

    public String getUsageShort(){
        return "Delete alerts for a resource or a time range" ;
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".\n" +
               "    To delete alerts for a resource:\n" +
               "      alert delete [" +
               ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
               " <resource>]\n\n" +
               "    To delete alerts in a time range:\n" +
               "      alert delete -from <date> [-to <date>]\n\n" + 
               "      If the -to flag is not specified, the current time " +
               "will be used.\n" +
               "      Dates and times can be specified in a number of ways.  " +
               "They can be\n" +
               "      called out explicitly (such as \"3:00pm\", " +
               "\"12/24/02 9:00pm\", \"monday\",\n" +
               "      \"yesterday\", or \"now\") and can be given offsets " +
               "(such as \"now - 1 day\", \n" +
               "      \"march + 4 days\", etc.)\n\n";
        
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals) {
        String blockName;

        ClientShellParseUtil.bubbleUpResourceBlock(result, blockVals);
        
        blockName = result.getBlockName();
        if(blockName.equals(BLOCK_FROM) || blockName.equals(BLOCK_TO)){
            DateParser dp = (DateParser)blockVals[1];

            result.getRoot().setValue(blockName, new Long(dp.getValue()));
        }
    }
}
