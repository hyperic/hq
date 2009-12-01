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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class RewriteWriter
    extends WriterHandler
{
    private File        destFile;
    private File        backupFile;
    private InputStream inStream;
    private long        length;
    private boolean     copied;

    RewriteWriter(File destFile, InputStream inStream, long length){
        super();

        this.destFile   = destFile;
        this.backupFile = null;
        this.inStream   = inStream;
        this.length     = length;
        this.copied     = false;
    }

    RewriteWriter(File destFile, byte[] data){
        this(destFile, new ByteArrayInputStream(data), data.length);
    }

    public void rollback()
        throws IOException
    {
        super.rollback();

        if(this.backupFile != null){
            if(this.copied)
                copy(this.backupFile, this.destFile);
            this.backupFile.delete();
        }
    }

    public void cleanup(){
        super.cleanup();

        this.backupFile.delete();
    }

    public void write()
        throws IOException
    {
        FileOutputStream fOs;

        super.write();
        if(!this.destFile.exists()){
            throw new FileNotFoundException(this.destFile + " does not " +
                                            "exist for rewrite");
        }

        this.backupFile = File.createTempFile("fwrite", "tmp");
        copy(this.destFile, this.backupFile);
        this.copied = true;

        fOs = new FileOutputStream(this.destFile);
        try {
            copyStream(this.inStream, fOs, this.length);
        } finally {
            try {fOs.close();} catch(IOException exc){}
        }
    }

    private static void copy(File in, File out) 
        throws IOException 
    {
        FileInputStream  is = new FileInputStream(in);
        FileOutputStream os = new FileOutputStream(out);
        int count;
        byte b[];
        
        count = 0;
        b = new byte[8192];    
        while((count = is.read(b)) != -1){
            os.write(b, 0, count);
        }
        is.close();
        os.close();
    }
}
