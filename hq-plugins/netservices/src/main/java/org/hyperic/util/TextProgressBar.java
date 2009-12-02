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

package org.hyperic.util;

import java.io.PrintStream;

public class TextProgressBar {
    private PrintStream out;
    private PrintfFormat printf;
    private String erase = null;
    private String name;
    private long current, total, startTime;
    private long refresh, lastUpdate;
    private boolean isComplete = false;

    private static final int NAME_LEN = 20;
    private static final String fmt = "%-" + NAME_LEN + "s | " +
        "%+8s kB | %4.2f kB/s | ETA: %-10s | %3d %%";
    /**
     * Initialize the progress bar
     *
     * @param os Stream to use for writing the progress status
     * @param name The name of the file being transferred
     * @param total Total bytes for completion
     */
    public TextProgressBar(PrintStream out, String name, long total) {
        this.out = out;
        this.total = total;
        this.name = name;
        this.current = 0;
        this.lastUpdate = 0;
        this.refresh = 1000; // 1 second
        this.startTime = System.currentTimeMillis() - 1;
        this.printf = new PrintfFormat(fmt);
    }

    public void setRefresh(long refresh) {
        this.refresh = refresh;
    }

    public void update(long current) {
        this.current = current;
    }

    public void print(long current) {
        this.current = current;
        print();
    }

    public void print() {
        if (this.isComplete) {
            return;
        }
        // Total Kb transferred
        Long total = new Long(this.current/1024);
        
        // Pecent left
        Integer percent = new Integer((int)(this.current*100/this.total));

        // Only allow 1 update per refresh period.  Allow the update to happen
        // though if the transfer is complete, and we have not sent the newline
        long now = System.currentTimeMillis();
        if ((percent.intValue() < 100) &&
            (now - this.lastUpdate) < this.refresh) {
            return;
        }
        
        // Calcuate transfer rate
        long elapsed = now - this.startTime;
        Double rate = new Double((this.current)/((elapsed*1024)/1000));

        // Format time left
        String timeLeft;
        if (rate.longValue() == 0) {
            timeLeft = "N/A";
        } else {
            long remain = (this.total - this.current) / (rate.longValue());
            timeLeft = StringUtil.formatDuration(remain);
        }

        //Format file name
        String filename;
        if (name.length() > NAME_LEN)
            filename = name.substring(0, NAME_LEN);
        else
            filename = name;

        Object[] args = { filename,
                          total.toString(), 
                          rate,
                          timeLeft,
                          percent
        };
        
        String status = printf.sprintf(args);
        if (this.erase == null) {
            out.print(status);
            this.erase = StringUtil.repeatChars('\b', status.length());
        } else {
            out.print(erase);
            if (percent.intValue() == 100) {
                out.println(status);  // Only print the newline once
                this.isComplete = true;
            } else {
                out.print(status);
            }
            this.erase = StringUtil.repeatChars('\b', status.length());
        }
        this.lastUpdate = now;
    }

    public static void main(String args[]) 
        throws Exception
    {
        String name = "test.jar"; // file to transfer
        long size   = 2 * 1024 * 1024;

        System.out.println("Transferring " + name);
        TextProgressBar progress = new TextProgressBar(System.out, name, size);

        int i; // simulates the # of bytes sent
        for (i = 0; i <= size; i=i+8192) {
            Thread.sleep(10);
            progress.print(i);
        }
        progress.print(i);

        System.out.println("Done");
    }
}
