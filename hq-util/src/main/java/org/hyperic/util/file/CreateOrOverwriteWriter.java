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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class CreateOrOverwriteWriter
    extends WriterHandler
{
    private File        destFile;
    private File        backupFile;
    private InputStream inStream;
    private long        length;
    private boolean     copied;
    private boolean     created;

    CreateOrOverwriteWriter(File destFile, InputStream inStream, long length){
        super();
        
        this.destFile   = destFile;
        this.backupFile = null;
        this.inStream   = inStream;
        this.length     = length;
        this.copied     = false;
        this.created    = false;
    }

    CreateOrOverwriteWriter(File destFile, byte[] data){
        this(destFile, new ByteArrayInputStream(data), data.length);
    }

    public void rollback()
        throws IOException
    {
        super.rollback();

        if(this.backupFile != null){
            // If this was an overwrite
            if(this.backupFile.renameTo(destFile) == false){
                // Try a copy now
                try {
                    FileUtil.copyStream(new FileInputStream(this.backupFile),
                                        new FileOutputStream(this.destFile));
                } catch(IOException exc){
                    this.backupFile.delete();
                    throw exc;
                }
            }
        } else {
            // Else, we created the file.  Make sure it's nuked.
            if(this.created == true)
                this.destFile.delete();
        }
    }

    private void setupBackup()
        throws IOException
    {
        File parentDir;

        parentDir = this.destFile.getAbsoluteFile().getParentFile();

        if(parentDir == null){
            throw new IOException("Unable to get the owner directory for " +
                                  this.destFile);
        }

        if(this.destFile.exists()){
            if(!this.destFile.isFile()){
                throw new IOException(destFile + " is not a regular file");
            }
            
            // Try within the same directory, first
            try {
                this.backupFile = File.createTempFile("fwrite", "tmp", 
                                                      parentDir);
            } catch(IOException exc){
                // Else use the system temp directory
                this.backupFile = File.createTempFile("fwrite", "tmp");
            }

            if(this.destFile.renameTo(this.backupFile) == false){
                // If this fails, attempt a copy
                try {
                    FileUtil.copyStream(new FileInputStream(this.destFile),
                                        new FileOutputStream(this.backupFile));
                    this.copied = true;
                } catch(IOException exc){
                    throw new IOException("Unable to rename " + this.destFile +
                                          " to " + this.backupFile + 
                                          " for backup purposes");
                }
            }
        }
    }

    public void cleanup(){
        super.cleanup();

        if(this.backupFile != null){
            this.backupFile.delete();
            this.backupFile = null;
        }
    }

    public void write()
        throws IOException
    {
        FileOutputStream fOs;

        super.write();
        this.setupBackup();
        
        if(!this.copied){
            if(this.destFile.createNewFile() == false){
                throw new IOException("Unable to create " + this.destFile);
            }
            this.created = true;
        }

        fOs = new FileOutputStream(this.destFile);
        try {
            copyStream(this.inStream, fOs, this.length);
        } finally {
            try { fOs.close(); } catch(IOException exc){}
        }
    }
}
