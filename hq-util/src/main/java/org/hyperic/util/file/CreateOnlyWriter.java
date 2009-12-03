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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class CreateOnlyWriter
    extends WriterHandler
{
    private File        destFile;
    private InputStream inStream;
    private boolean     created;
    private long        length;

    CreateOnlyWriter(File destFile, InputStream inStream, long length){
        super();

        this.destFile  = destFile;
        this.inStream  = inStream;
        this.created   = false;
        this.length    = length;
    }

    CreateOnlyWriter(File destFile, byte[] data){
        this(destFile, new ByteArrayInputStream(data), data.length);
    }

    public void rollback()
        throws IOException
    {
        super.rollback();

        if(this.created)
            this.destFile.delete();
    }

    public void cleanup(){
        super.cleanup();
    }

    public void write()
        throws IOException
    {
        FileOutputStream fOs;

        super.write();

        if(this.destFile.createNewFile() == false){
            throw new IOException("Unable to create " + this.destFile + 
                                  ": file already exists");
        }

        this.created = true;
        fOs = new FileOutputStream(this.destFile);
        try {
            copyStream(this.inStream, fOs, this.length);
        } finally {
            try { fOs.close(); } catch(IOException exc){}
        }
    }
}
