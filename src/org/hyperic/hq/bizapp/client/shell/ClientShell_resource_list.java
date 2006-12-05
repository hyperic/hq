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

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllAppsFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllPlatformsFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServersFetcher;
import org.hyperic.hq.bizapp.client.pageFetcher.FindAllServicesFetcher;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;

import org.hyperic.util.PrintfFormat;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.shell.ShellCommandBase;
import org.hyperic.util.shell.ShellCommandExecException;
import org.hyperic.util.shell.ShellCommandUsageException;

import javax.ejb.FinderException;
import javax.naming.NamingException;

import java.io.PrintStream;
import java.rmi.RemoteException;
import java.util.List;

public class ClientShell_resource_list 
    extends ShellCommandBase 
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_APP,
        ClientShell_resource.PARAM_GROUP,
        ClientShell_resource.PARAM_PLATFORM,
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };

    private ClientShellEntityFetcher entityFetcher;

    public ClientShell_resource_list(ClientShell shell){
        this.entityFetcher = 
            new ClientShellEntityFetcher(shell.getBossManager(),
                                      shell.getAuthenticator());
    }

    private void printApps(PageControl control)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindAllAppsFetcher aFetch;
        ValuePrinterPageFetcher vpFetch;
        ValuePrinter printer;

        aFetch = this.entityFetcher.findAllAppsFetcher();
        printer = new ValuePrinter("%-7s %-15s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_DESCRIPTION
                                   });
        printer.setHeaders(new String[] { "ID", "Name", "Description" });
        printer.setPrologue("[ Application listing ]");
        vpFetch = new ValuePrinterPageFetcher(aFetch, printer);
        this.getShell().performPaging(vpFetch, control);
    }

    private void printGroups(PrintStream out)
        throws NamingException, FinderException,
               SessionTimeoutException, SessionNotFoundException, 
               RemoteException, PermissionException,
               ClientShellAuthenticationException, SystemException,
               ApplicationException
    {
        ValuePrinter printer;
        List groups;

        groups = entityFetcher.findAllGroups();
        printer = new ValuePrinter(out, 
                                   new int[] {
                                       ValueWrapper.ATTR_ID, 
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_TYPENAME, 
                                       ValueWrapper.ATTR_DESCRIPTION});
        printer.setHeaders(new String[] { "Id", "Name", "Type", 
                                          "Description"} );
        printer.setPrologue("[ Group listing ]");
        printer.printList(groups);
    }

    private void printPlatforms(PageControl control)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindAllPlatformsFetcher pFetch;
        ValuePrinterPageFetcher vpFetch;
        ValuePrinter printer;

        pFetch = this.entityFetcher.findAllPlatformsFetcher();
        printer = new ValuePrinter("%-7s %-15s %-20s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_TYPENAME,
                                       ValueWrapper.ATTR_FQDN
                                   });
        printer.setHeaders(new String[] { "ID", "Name", "Platform Type",
                                          "Domain Name" });
        printer.setPrologue("[ Platform listing ]");
        vpFetch = new ValuePrinterPageFetcher(pFetch, printer);
        this.getShell().performPaging(vpFetch, control);
    }

    private void printServers(PageControl control)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindAllServersFetcher sFetch;
        ValuePrinterPageFetcher vpFetch;
        ValuePrinter printer;

        sFetch = this.entityFetcher.findAllServersFetcher();
        printer = new ValuePrinter("%-7s %-40s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_TYPENAME
                                   });
        printer.setHeaders(new String[] { "ID", "Name", "Server Type" });
        printer.setPrologue("[ Server listing ]");
        vpFetch = new ValuePrinterPageFetcher(sFetch, printer);
        this.getShell().performPaging(vpFetch, control);
    }

    private void printServices(PageControl control)
        throws NamingException, ClientShellAuthenticationException,
               PageFetchException
    {
        FindAllServicesFetcher sFetch;
        ValuePrinterPageFetcher vpFetch;
        ValuePrinter printer;

        sFetch = this.entityFetcher.findAllServicesFetcher();
        printer = new ValuePrinter("%-7s %-40s %s",
                                   new int[] {
                                       ValueWrapper.ATTR_ID,
                                       ValueWrapper.ATTR_NAME,
                                       ValueWrapper.ATTR_TYPENAME
                                   });
        printer.setHeaders(new String[] { "ID", "Name", "Service Type" });
        printer.setPrologue("[ Service listing ]");
        vpFetch = new ValuePrinterPageFetcher(sFetch, printer);
        this.getShell().performPaging(vpFetch, control);
    }

    public void processCommand(String[] args) 
        throws ShellCommandUsageException, ShellCommandExecException 
    {
        // First, check all the args
        for(int i=0; i<args.length; i++){
            if(!ClientShell_resource.paramIsValid(PARAM_VALID_RESOURCE, args[i])){
                throw new ShellCommandUsageException(this.getSyntax());
            }
        }

        try {
            PrintStream out = this.getOutStream();
            PageControl control;

            if(args.length == 0){
                control = new PageControl(0, PageControl.SIZE_UNLIMITED);
                this.printApps(control);
                this.printGroups(out);
                this.printPlatforms(control);
                this.printServers(control);
                this.printServices(control);
            } else {
                control = this.getShell().getDefaultPageControl();

                for(int i=0; i<args.length; i++){
                    int id = ClientShell_resource.convertParamToInt(args[i]);
                    
                    switch(id){
                    case ClientShell_resource.PARAM_APP:
                        this.printApps(control);
                        break;
                    case ClientShell_resource.PARAM_GROUP:
                        this.printGroups(out);
                        break;
                    case ClientShell_resource.PARAM_PLATFORM:
                        this.printPlatforms(control);
                        break;
                    case ClientShell_resource.PARAM_SERVER:
                        this.printServers(control);
                        break;
                    case ClientShell_resource.PARAM_SERVICE:
                        this.printServices(control);
                        break;
                    default:
                        throw new IllegalStateException("Unhandled resource");
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
        return "List " + ClientShell.PRODUCT + " resources by type";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".  If no argument is " +
            "specified, all\n" +
            "    resources will be returned";
    }
}
