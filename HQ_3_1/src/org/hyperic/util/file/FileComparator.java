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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileComparator {

    public boolean compare ( File file1, File file2 ) throws IOException {

        FileInputStream f1 = null;
        FileInputStream f2 = null;
        byte[] buf1 = new byte[4096];
        byte[] buf2 = new byte[4096];
        int read1, read2;

        try {
            f1 = new FileInputStream(file1);
            f2 = new FileInputStream(file2);
            read1 = f1.read(buf1);
            read2 = f2.read(buf2);

            if (read1 == -1) return (read2 == -1);
            if (read2 == -1) return false;

            while (true) {

                if ( !Arrays.equals(buf1, buf2) ) return false;

                read1 = f1.read(buf1);
                read2 = f2.read(buf2);

                if (read1 == -1) return (read2 == -1);
                if (read2 == -1) return false;
            }

        } finally {
            try { if (f1 != null) f1.close(); } catch ( Exception e ) {}
            try { if (f2 != null) f2.close(); } catch ( Exception e ) {}
        }
    }
}
