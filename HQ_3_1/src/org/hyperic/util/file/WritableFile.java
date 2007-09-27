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
import java.net.URI;

/**
 * Instances of this class are returned from
 * FileUtil.findWritableFile
 */
public class WritableFile extends File {

    private boolean _originalLocationWasUsed = true;

    public WritableFile(File parent, String child) {
        super(parent, new File(child).getName());
    }
    public WritableFile(String parent, String child) {
        super(parent, new File(child).getName());
    }
    public WritableFile(String pathname) {
        super(pathname);
    }
    public WritableFile(URI uri) {
        super(uri);
    }

    /**
     * Create any necessary directories needed to write this file.
     */
    public boolean mkdirs () {
        if (exists()) return true;
        if ( !isDirectory() ) {
            File parent = getParentFile();
            if (parent.exists()) return true;
            return parent.mkdirs();
        }
        return super.mkdirs();
    }

    /** 
     * @return true if the original desired location
     * for this file was used.
     */
    public boolean getOriginalLocationWasUsed () {
        return _originalLocationWasUsed;
    }
    public void setOriginalLocationWasUsed ( boolean orig ) {
        _originalLocationWasUsed = orig;
    }
}
