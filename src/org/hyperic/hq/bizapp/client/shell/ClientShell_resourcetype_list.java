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
import javax.naming.NamingException;

import org.hyperic.hq.bizapp.client.pageFetcher.FindAllPlatformTypesFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServerTypesFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServiceTypesFetcher;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_resourcetype_list 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };
    
    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_resourcetype_list(ClientShell shell){
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    private void printPlatformTypes(PageControl control)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindAllPlatformTypesFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;
        ValuePrinter printer;

        pFetch = this.entityFetcher.findAllPlatformTypesFetcher();
        printer = new ValuePrinter("%-7s %-15s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_PLUGIN,
                                   });
        printer.setHeaders(new String[] { "ID", "Name", "Plugin"});
        printer.setPrologue("[ Platform type listing ]");

        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, control);
    }

    private void printServerTypes(PageControl control)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindAllServerTypesFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;
        ValuePrinter printer;

        pFetch = this.entityFetcher.findAllServerTypesFetcher();
        printer = new ValuePrinter("%-7s %-30s %-20s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_PLUGIN,
                                       ValueWrapper.ATTR_DESCRIPTION
                                   });

        printer.setHeaders(new String[] { "ID", "Name", "Plugin",
                                          "Description" });
        printer.setPrologue("[ Server type listing ]");

        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, control);
    }

    private void printServiceTypes(PageControl control)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindAllServiceTypesFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;
        ValuePrinter printer;

        pFetch = this.entityFetcher.findAllServiceTypesFetcher();
        printer = new ValuePrinter("%-7s %-35s %-20s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_PLUGIN,
                                       ValueWrapper.ATTR_DESCRIPTION,
                                   });

        printer.setHeaders(new String[] { "ID", "Name", "Plugin",
                                          "Description" });
        printer.setPrologue("[ Service type listing ]");

        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, control);
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        PrintStream out = this.getOutStream();
        PageControl control;

        // First, check all the args
        for(int i=0; i<args.length; i++){
            if(!ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, args[i])){
                throw new ShellCommandUsageException(this.getSyntax());
            }
        }

        try {
            if(args.length == 0){
                control = new PageControl(0, PageControl.SIZE_UNLIMITED);
                this.printPlatformTypes(control);
                this.printServerTypes(control);
                this.printServiceTypes(control);
            } else {
                control = this.getShell().getDefaultPageControl();

                for(int i=0; i<args.length; i++){
                    int id = ClientShell_resource.convertParamToInt(args[i]);

                    switch(id){
                    case ClientShell_resource.PARAM_PLATFORM:
                        this.printPlatformTypes(control);
                        break;
                    case ClientShell_resource.PARAM_SERVER:
                        this.printServerTypes(control);
                        break;
                    case ClientShell_resource.PARAM_SERVICE:
                        this.printServiceTypes(control);
                        break;
                    default:
                        throw new IllegalStateException("Only platforms, " +
                                                        "servers and " +
                                                        "services are " +
                                                        "supported");
                    }
                }
            }
        } catch(Exception exc){
            throw new ShellCommandExecException(exc);
        }
    }

    public String getSyntaxArgs(){
        return "[" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "]";
    }

    public String getUsageShort(){
        return "List " + ClientShell.PRODUCT + " resource types";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".  If no argument is " +
            "specified, all\n" +
            "    resource types will be returned";
    }
}
