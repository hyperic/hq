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
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.security.MD5;

/**
 * A class which has the ability to write to files, deal with
 * permissions/ownership, and rollback changes later on.
 */
public class FileWriter 
    extends WriterHandler
{
    private static final int MODE_CREATEONLY        = 1;
    private static final int MODE_CREATEOROVERWRITE = 2;
    private static final int MODE_REWRITE           = 3;
    
    private final Log log = LogFactory.getLog(FileWriter.class);

    private File                 destFile;
    private InputStream          inStream;
    private WriterHandler        writer;
    private int                  mode;
    private long                 length;
    private String               expectedMD5Sum;

    public FileWriter(File destFile, InputStream inStream, long size){
        super();
        this.destFile   = destFile;
        this.inStream   = inStream;
        this.length     = size;
        this.writer     = null;
        this.mode       = MODE_CREATEOROVERWRITE;
    }

    public FileWriter(File destFile, byte[] data){
        this(destFile, new ByteArrayInputStream(data), data.length);
    }
    
    /**
     * Verify the MD5 check sum on file write.
     * 
     * @param expectedMD5sum The expected MD5 check sum.
     */
    public void setVerifyMD5CheckSumOnWrite(String expectedMD5sum) {
        this.expectedMD5Sum = expectedMD5sum;
    }

    public File getDestFile(){
        return this.destFile;
    }

    public void setCreateOnly(){
        this.mode = MODE_CREATEONLY;
    }

    public boolean isCreateOnly(){
        return this.mode == MODE_CREATEONLY;
    }

    public void setCreateOrOverwrite(){
        this.mode = MODE_CREATEOROVERWRITE;
    }

    public boolean isCreateOrOverwrite(){
        return this.mode == MODE_CREATEOROVERWRITE;
    }

    public void setRewrite(){
        this.mode = MODE_REWRITE;
    }

    public boolean isRewrite(){
        return this.mode == MODE_REWRITE;
    }

    public void rollback()
        throws IOException
    {
        super.rollback();

        this.writer.rollback();
    }

    public void cleanup(){
        super.cleanup();

        this.writer.cleanup();
    }

    /**
     * Write the file.
     * 
     * @throws IOException if the file write fails.
     */
    public void write()
        throws IOException
    {
        super.write();
        if(this.isCreateOrOverwrite())
            this.writer = new CreateOrOverwriteWriter(this.destFile, 
                                                      this.inStream,
                                                      this.length);
        else if(this.isCreateOnly())
            this.writer = new CreateOnlyWriter(this.destFile, 
                                               this.inStream,
                                               this.length);
        else if(this.isRewrite())
            this.writer = new RewriteWriter(this.destFile, 
                                            this.inStream,
                                            this.length);
        else
            throw new IllegalStateException("Unhandled write mode");


        this.writer.write();
    }
    
    /**
     * Verify the MD5 check sum of the written file.
     * 
     * @throws IOException if the MD5 check sum verification fails.
     * @throws IllegalStateException if the file has not been written yet.
     */
    public void verifyMD5CheckSum() throws IOException {
        if (this.writer == null || !this.writer.hasWritten()) {
            throw new IllegalStateException("the file must be written before " +
            		"verifying the MD5 check sum");
        }
        
        if (this.expectedMD5Sum != null) {
            String actualMD5Sum = MD5.getMD5Checksum(getDestFile());
            
            log.debug("Verifying MD5 check sum for file "+getDestFile()+
                      "; expected="+this.expectedMD5Sum+", actual="+actualMD5Sum);
            
            if (!this.expectedMD5Sum.equals(actualMD5Sum)) {
                throw new IOException("MD5 check sum failed for file "+
                                      getDestFile()+"; expected="+this.expectedMD5Sum+
                                      ", actual="+actualMD5Sum);
            }
        }
    }

}
