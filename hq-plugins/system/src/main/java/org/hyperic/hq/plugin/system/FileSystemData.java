/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.plugin.system;

import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarException;

public class FileSystemData {

    private FileSystem _config;
    private FileSystemUsage _stat;

    public FileSystemData() {}

    public void populate(SigarProxy sigar, FileSystem fs)
        throws SigarException {

        _config = fs;

        try {
            _stat = sigar.getFileSystemUsage(fs.getDirName());
        } catch (SigarException e) {
            
        }
    }

    public static FileSystemData gather(SigarProxy sigar, FileSystem fs)
        throws SigarException {
    
        FileSystemData data = new FileSystemData();
        data.populate(sigar, fs);
        return data;
    }

    public FileSystem getConfig() {
        return _config;
    }

    public FileSystemUsage getStat() {
        return _stat;
    }
}
