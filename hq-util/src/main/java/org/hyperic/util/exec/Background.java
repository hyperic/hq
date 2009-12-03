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

package org.hyperic.util.exec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class to execute processes in the background.  These processes
 * will not exit once the controlling Java process exits.  
 */
public class Background {
    /**
     * Execute the command (and its args, ala Runtime.exec), sending the
     * output && error streams to the void.
     */
    public static void exec(String[] cmd)
        throws IOException
    {
        File devNull;
        if(Os.isFamily("unix")) 
            devNull = new File("/dev/null");
        else if (Os.isFamily("windows")) 
            devNull = new File("NUL");
        else
            throw new IllegalStateException("Unhandled Java environment");
        exec(cmd, devNull, false, devNull, false);
    }

    /**
     * Execute a command (and its args, ala Runtime.exec)
     *
     * @param outFile    File to send standard out from the process to
     * @param appendOut  If true, append the file with standard out,
     *                   else truncate or create a new file
     * @param errFile    File to send standard err from the process to
     * @param appendErr  If true, append the file with standard error, 
     *                   else truncate or create a new file
     */
    public static void exec(String[] cmd, 
                            File outFile, boolean appendOut,
                            File errFile, boolean appendErr)
        throws IOException
    {
        if(Os.isFamily("unix"))
            execUnix(cmd, outFile, appendOut, errFile, appendErr);
        else if (Os.isFamily("windows"))
            execWin(cmd, outFile, appendOut, errFile, appendErr);
        else
            throw new IllegalStateException("Unhandled Java environment");
    }

    private static void execUnix(String[] cmd, 
                                 File outFile, boolean appendOut, 
                                 File errFile, boolean appendErr)
        throws IOException
    {
        StringBuffer escaped;
        String[] execCmd;
        Runtime r;

        escaped = new StringBuffer();
        for(int i=0; i<cmd.length; i++){
            escaped.append(Escape.escape(cmd[i]));
            escaped.append(" ");
        }

        execCmd = new String[] {
            "/bin/sh",
            "-c",
            escaped.toString() + 
            (appendOut == true ? ">>" : ">") + 
            Escape.escape(outFile.getAbsolutePath()) +
            " 2" + (appendErr == true ? ">>" : " >") +
            Escape.escape(errFile.getAbsolutePath()) + 
            " </dev/null &"
        };

        Process p = Runtime.getRuntime().exec(execCmd);
        try {
            p.waitFor();
        } catch(Exception exc){
            throw new IOException("Unable to properly background process: " +
                                  exc.getMessage());
        }
    }

    private static void execWin(String[] cmd, 
                                File outFile, boolean appendOut, 
                                File errFile, boolean appendErr)
        throws IOException
    {
        String[] logargs;
        String[] execCmd;
        ArrayList tmpCmd = new ArrayList();
        Runtime r;

        tmpCmd.add("cmd");
        tmpCmd.add("/c");
        tmpCmd.add("start");
        tmpCmd.add("/b");
        tmpCmd.add("\"\"");
        tmpCmd.add("/MIN");
        for(int i=0; i<cmd.length; i++){
            tmpCmd.add(cmd[i]);
        }
        tmpCmd.add((appendOut == true ? ">>" : ">") + 
            Escape.escape(outFile.getAbsolutePath()));
        tmpCmd.add((outFile.equals(errFile) ? " 2&" : " 2") + 
            (appendErr == true ? ">>" : " >") +
            Escape.escape(errFile.getAbsolutePath())); 

        Process p = Runtime.getRuntime().exec((String [])tmpCmd.toArray(cmd));
    }

    public static void main(String[] args) throws Exception {
        Background.exec(new String[] {"javaq", "foo bar", "bar" },
                        new File("garfo"), true, new File("barfo"), true);
    }
}
