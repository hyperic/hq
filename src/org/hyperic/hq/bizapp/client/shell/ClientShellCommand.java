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

import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.paramParser.BlockHandler;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParamParser;
import org.hyperic.util.paramParser.ParseException;
import org.hyperic.util.paramParser.ParseResult;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

/**
 * Subclass which makes shell commands handle their parameter
 * parsing via the ParamParser.  Also provides utility 
 * functions such as getting an entity fetcher, etc.
 */
public abstract class ClientShellCommand 
    extends ShellCommandBase
    implements BlockHandler
{
    private ClientShellEntityFetcher   entityFetcher;
    private ParamParser             paramParser;
    private ClientShell                shell;

    public ClientShellCommand(ClientShell shell, String paramFormat){
        ClientShellParserRetriever retriever;

        this.entityFetcher =
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
        retriever = new ClientShellParserRetriever(this.entityFetcher);
        this.paramParser = new ParamParser(paramFormat, retriever, this);
        this.shell = shell;
    }
    
    public ClientShell getClientShell(){
        return this.shell;
    }

    protected ClientShellEntityFetcher getEntityFetcher(){
        return this.entityFetcher;
    }

    /**
     * handleBlock is called when the paramParser successfully parses
     * a block.  
     * 
     * @param result    The parseResult which is the result of parsing
     *                  the block
     * @param blockVals Individual entities making up the block which 
     *                  was just parsed.
     */
    public void handleBlock(ParseResult result, FormatParser[] blockVals){
    }

    protected abstract void processCommand(ParseResult parseRes)
        throws ShellCommandUsageException, ShellCommandExecException;

    public void processCommand(String[] args)
        throws ShellCommandUsageException, ShellCommandExecException
    {
        ParseResult parseRes;

        try {
            parseRes = this.paramParser.parseParams(args);
        } catch(ParseException exc){
            if(exc.getExceptionOfType(ApplicationException.class) != null ||
               exc.getExceptionOfType(SystemException.class) != null)
            {
                throw new ShellCommandExecException(exc);
            }

            if(this.getClientShell().isDeveloper()){
                exc.printStackTrace();
            }
            throw new ShellCommandUsageException(this.getSyntaxEx());
        }

        this.processCommand(parseRes);
    }

    /**
     * Get a simple 'Use 'help blah' for details' string, to use in
     * exceptions when the real syntax is too long.
     */
    protected String getSyntaxEx(){
        return "Use 'help " + this.getCommandName() + "' for details";
    }
}
