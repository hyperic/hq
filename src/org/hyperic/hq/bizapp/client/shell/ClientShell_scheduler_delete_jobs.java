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

import gnu.getopt.Getopt;

import org.hyperic.hq.bizapp.shared.SchedulerBoss;
import org.hyperic.util.shell.*;

public class ClientShell_scheduler_delete_jobs extends MultiwordShellCommand {
    private ClientShell_scheduler_delete owner;
    private ClientShellAuthenticator auth;

    public ClientShell_scheduler_delete_jobs(ClientShell_scheduler_delete owner,
                                          ClientShellAuthenticator eamshellauthenticator)
    {
        this.owner = owner;
        auth = eamshellauthenticator;
    }

    public void processCommand(String args[])
        throws ShellCommandUsageException, ShellCommandExecException
    {
        SchedulerBoss boss = owner.getOwner().getSchedulerBoss();

        // parse options
        Getopt parser = new Getopt("jobs", args, "g:j:");
        int c;
        parser.setOpterr(true);
        String groupName = null;
        String jobName = null;
        while ( (c = parser.getopt() ) != -1 ) {
            String optionValue = parser.getOptarg();
            switch(c){
            case 'g':
                groupName = optionValue;
                break;
            case 'j':
                jobName = optionValue;
                break;
            default:
                throw new ShellCommandUsageException("Invalid or missing options.");
            }
        }

        try {
            int sessionID = auth.getAuthToken();
            PrintStream out = this.getOutStream();

            if (null != groupName) {
                if (null != jobName) {
                    boolean deleted = boss.deleteJob(sessionID, jobName, groupName);
                    if (deleted) {
                        out.println("Job deleted succesfully.");
                    } else {
                        out.println("Job could not be deleted.");
                    }
                } else {
                    int numDeleted = boss.deleteJobGroup(sessionID, groupName);
                    out.println("Deleted " + numDeleted + " jobs.");
                }
            } else {
                throw new ShellCommandUsageException
                    ("Must specify at least a job group.");
            }
        } catch (ShellCommandUsageException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ShellCommandExecException(e);
        }
    }

    public String getUsageHelp(String as[]) {
        return "scheduler delete jobs < -g groupName > [ -j jobName ]";
    }
}

// EOF
