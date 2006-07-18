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

package org.hyperic.util.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.hyperic.util.math.MathUtil;

class WriterHandler {
    private boolean rolledBack;
    private boolean cleanedUp;
    private boolean wrote;

    WriterHandler(){
        this.rolledBack = false;
        this.cleanedUp  = false;
        this.wrote      = false;
    }

    public void cleanup(){
        if(this.cleanedUp){
            throw new IllegalStateException("Cleanup called twice");
        }

        if(!this.wrote){
            throw new IllegalStateException("Cleanup called when not written");
        }

        this.cleanedUp = true;
    }

    public void rollback() 
        throws IOException
    {
        if(this.rolledBack){
            throw new IllegalStateException("Rollback called twice");
        }

        if(!this.wrote){
            throw new IllegalStateException("Rollback called when not " +
                                            "written");
        }

        if(this.cleanedUp){
            throw new IllegalStateException("Rolled back after cleaning up");
        }

        this.rolledBack = true;
    }


    public void write() 
        throws IOException
    {
        if(this.wrote){
            throw new IllegalStateException("Write called twice");
        }

        this.wrote = true;
    }

    protected static void copyStream(InputStream is, OutputStream os,
                                     long totalToCopy)
        throws IOException
    {
        int count;
        byte[] buf = new byte[8192];
        long total = 0;
        long remain = totalToCopy;
    
        while((total != totalToCopy) &&
              (count = is.read(buf, 0, 
                               (int)MathUtil.clamp(remain, 0, buf.length))) != -1) {
            os.write(buf, 0, count);
            total += count;
            remain -= count;
        }
    
        // Throw IOException on short reads
        if (totalToCopy != total)
            throw new IOException("Short read, expected=" + totalToCopy +
                                  " read=" + total);
    }
}
