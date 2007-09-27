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

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.paramParser.StringParser;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_resource_set
    extends ClientShellCommand
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_APP,
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
        ClientShell_resource.PARAM_GROUP
    };
    
    private static final String PARAM_FILE   = "-file";

    private static final String BLOCK_KEY    = "Key";
    private static final String BLOCK_VALUE  = "Value";
    private static final String PARAM_FORMAT =
        "<" + ClientShellParseUtil.makeResourceBlock(PARAM_VALID_RESOURCE) +
        "> <$" + BLOCK_KEY + " #String> <$" + BLOCK_VALUE + " #String>" +
        " [" + PARAM_FILE + "]";

    /** The size of blocking to use */
    private static final int BLKSIZ = 8192;

    public ClientShell_resource_set(ClientShell shell){
        super(shell, PARAM_FORMAT);
    }

    public void handleBlock(ParseResult result, FormatParser[] blockVals){
        ClientShellParseUtil.bubbleUpResourceBlock(result, blockVals);
        
        if(blockVals[0] instanceof StringParser){
            StringParser sp = (StringParser)blockVals[0];

            if(sp.getValue().equals(PARAM_FILE)){
                result.getRoot().setValue(PARAM_FILE, sp.getValue());
            } else {
                result.getRoot().setValue(result.getBlockName(), 
                                          sp.getValue());
            }
        }
    }

    public void processCommand(ParseResult parseRes) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        final String resourceKey = ClientShellParseUtil.KEY_RESOURCE;
        AppdefEntityID id;
        String key, value, file;

        id    = (AppdefEntityID)parseRes.getValue(resourceKey);
        key   = (String)parseRes.getValue(BLOCK_KEY);
        value = (String)parseRes.getValue(BLOCK_VALUE);
        file  = (String)parseRes.getValue(PARAM_FILE);
        
        if(file != null){
            try {
                value = ClientShell_resource_set.readerToString(new FileReader(value));
            } catch(Exception exc){
                this.getErrStream().println("Unable to read file '" + 
                                            value + "': " + exc.getMessage());
                return;
            }
        }

        try {
            this.getEntityFetcher().setCPropValue(id, key, value);
        } catch(CPropKeyNotFoundException exc){
            this.getErrStream().println(exc.getMessage());
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    public String getUsageShort(){
        return "Set a resource's custom properties";
    }

    public String getUsageHelp(String[] args){
        String cmdSpace;

        cmdSpace = StringUtil.repeatChars(' ', this.getCommandName().length());
        return "    " + this.getUsageShort() + ".  The properties which are "+
            "set for\n" +
            "    a resource can be viewed via the 'resource view' command, " +
            "and using\n    the " + ClientShell_resource_view.PARAM_CPROPS + 
            " parameter.\n\n" +
            "    To use the data in a file as the value for the key, use " +
            "the " + PARAM_FILE + " flag.\n\n" +
            "    Syntax: " + this.getCommandName() + 
            " <" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) + 
            ">\n" + cmdSpace + "             <resource> <key> <value> [" +
            PARAM_FILE + "]";
    }

    /** Read the entire content of a Reader into a String */
    private static String readerToString(Reader is) throws IOException {
        StringBuffer sb = new StringBuffer();
        char[] b = new char[BLKSIZ];
        int n;
    
        // Read a block. If it gets any chars, append them.
        while ((n = is.read(b)) > 0) {
            sb.append(b, 0, n);
        }
        is.close();
        // Only construct the String object once, here.
        return sb.toString();
    }
}
