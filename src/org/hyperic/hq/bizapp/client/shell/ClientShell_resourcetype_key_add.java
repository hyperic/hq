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

import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.CPropKeyExistsException;
import org.hyperic.hq.appdef.shared.CPropKeyValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.paramParser.StringParser;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_resourcetype_key_add
    extends ClientShellCommand
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    
    private static final String BLOCK_KEY   = "Key";
    private static final String BLOCK_DESC  = "Desc";
    private static final String PARAM_FORMAT =
        "<" + ClientShellParseUtil.makeResourceTypeBlock(PARAM_VALID_RESOURCE) +
        "> <$" + BLOCK_KEY + " #String> <$" + BLOCK_DESC + " #String>";

    public ClientShell_resourcetype_key_add(ClientShell shell){
        super(shell, PARAM_FORMAT);
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        String blockName;

        ClientShellParseUtil.bubbleUpResourceTypeBlock(result, blockVals);
        
        blockName = result.getBlockName();
        if(blockName.equals(BLOCK_KEY) || blockName.equals(BLOCK_DESC)){
            StringParser sp = (StringParser)blockVals[0];

            result.getRoot().setValue(blockName, sp.getValue());
        }
    }

    public void processCommand(ParseResult parseRes) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        final String resourceKey = ClientShellParseUtil.KEY_RESOURCETYPE;
        AppdefResourceTypeValue resource;
        CPropKeyValue propKey;
        String key, desc;

        resource = (AppdefResourceTypeValue)parseRes.getValue(resourceKey);
        key      = (String)parseRes.getValue(BLOCK_KEY);
        desc     = (String)parseRes.getValue(BLOCK_DESC);
        
        propKey = new CPropKeyValue(null, resource.getAppdefTypeId(),
                                    resource.getId().intValue(), key, desc);
        try {
            this.getEntityFetcher().addCPropKey(propKey);
        } catch(CPropKeyExistsException exc){
            this.getErrStream().println(exc.getMessage());
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    public String getUsageShort(){
        return "Add a custom property key to a resource type";
    }

    public String getUsageHelp(String[] args){
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".  Custom properties which\n" +
            "    can be set for resources must have a key first declared " +
            "for their resource\n" +
            "    type.  Use the 'resourcetype view' command to view the " +
            "current keys.\n\n" +
            "    Syntax: " + this.getCommandName() + 
            " <" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) + 
            ">\n" + cmdSpace + "             <resourceType> <key> <desc>";
    }
}
