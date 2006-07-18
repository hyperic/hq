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


import gnu.getopt.Getopt;

import org.hyperic.util.shell.ShellCommandUsageException;

public class ClientShell_list_args {
    public static final String FLAG_NONE        = "";
    public static final String FLAG_ALL         = "e::";
    public static final String FLAG_APPLICATION = "a:";
    public static final String FLAG_PLATFORM    = "p:";
    public static final String FLAG_SERVER      = "S:";
    public static final String FLAG_SERVICE     = "s:";

    public static final int OPT_NONE        = 0;
    public static final int OPT_APPLICATION = 1 << 0;
    public static final int OPT_PLATFORM    = 1 << 1;
    public static final int OPT_SERVER      = 1 << 2;
    public static final int OPT_SERVICE     = 1 << 3;
    public static final int OPT_ALL         = 1 << 4;

    public int    optionSpecified = OPT_NONE;
    public String optionValue     = null;

    /**
     * check if the first item is within the argument list
     * @param args
     * @param flags
     * @return ClientShell_list_args
     * @throws ShellCommandUsageException
     */
    public static ClientShell_list_args parse (String[] args, String flags)
        throws ShellCommandUsageException 
    {
        ClientShell_list_args res = new ClientShell_list_args();
        Getopt parser = new Getopt("list", args, flags);

        parser.setOpterr(false);
        int c = -1;
        
        if ( (c = parser.getopt()) != -1 ) {
//        while((c = parser.getopt()) != -1){
//            if(res.optionValue != null)
//                throw new ShellCommandUsageException("Too many options: -" +
//                                                     (char)c);

			// if option has option value, get the option value
           	res.optionValue = parser.getOptarg();

            switch(c){
            case 'e':
                res.optionSpecified = ClientShell_list_args.OPT_ALL;
                break;
            case 'a':
                res.optionSpecified = ClientShell_list_args.OPT_APPLICATION;
                break;
            case 'p':
                res.optionSpecified = ClientShell_list_args.OPT_PLATFORM;
                break;
            case 'S':
                res.optionSpecified = ClientShell_list_args.OPT_SERVER;
                break;
            case 's':
                res.optionSpecified = ClientShell_list_args.OPT_SERVICE;
                break;
            default:
                throw new ShellCommandUsageException("Illegal option: -" +
                                                     (char)c);
            }
        } 

        return res;
    }
}
