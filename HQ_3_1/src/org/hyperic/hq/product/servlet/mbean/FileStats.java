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

package org.hyperic.hq.product.servlet.mbean;

import java.io.File;

/**
 * Provide generic File metrics via JMX.
 *
 * @jmx:mbean
 */
public class FileStats implements FileStatsMBean {

    private static final Long AVAIL_DOWN = new Long(0);
    private static final Long AVAIL_UP   = new Long(1);

    private File file;

    public FileStats() {}

    public FileStats(String name) {
        this(new File(name));
    }

    public FileStats(File file) {
        this.file = file;
    }

    /**
     * @jmx:managed-attribute
     */ 
    public Long getLastModified() {
        return new Long(this.file.lastModified());
    }

    /**
     * @jmx:managed-attribute
     */ 
    public Long getSize() {
        return new Long(this.file.length());
    }

    /**
     * @jmx:managed-attribute
     */ 
    public Long getAvailability() {
        return this.file.exists() ? AVAIL_UP : AVAIL_DOWN;
    }
}
