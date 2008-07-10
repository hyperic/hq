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

package org.hyperic.hq.agent.commands;

import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class AgentReceiveFileData_args 
    extends AgentRemoteValue 
{
    private static final String PROP_NFILES    = "numFiles";
    private static final String PROP_FILENAME  = "file.";
    private static final String PROP_FILESIZE  = "size.";
    private static final String PROP_WRITETYPE = "type.";
    private static final String PROP_MD5SUM    = "md5sum.";
    
    public AgentReceiveFileData_args(){
        super();
        this.setNumFiles(0);
    }

    public AgentReceiveFileData_args(AgentRemoteValue args)
        throws AgentRemoteException
    {
        super();

        int nFiles;

        this.setNumFiles(0);
        nFiles = getNumFiles(args);
        for(int i=0; i<nFiles; i++){
            FileData fileData = getFileData(args, i);
            this.addFileData(fileData);
        }
    }

    public void addFileData(FileData data)
        throws AgentRemoteException
    {
        int numFiles;

        numFiles = this.getNumFiles();
        this.setFileName(numFiles, data.getDestFile());
        this.setFileSize(numFiles, data.getSize());
        this.setWriteType(numFiles, data.getWriteType());
        
        String md5sum = data.getMD5CheckSum();
        
        if (md5sum != null) {
            this.setMD5CheckSum(numFiles, md5sum);    
        }
        
        this.setNumFiles(numFiles + 1);
    }
    
    public FileData getFile(int idx)
        throws AgentRemoteException
    {
        return getFileData(this, idx);
    }

    public int getNumFiles()
        throws AgentRemoteException
    {
        return getNumFiles(this);
    }
    
    private FileData getFileData(AgentRemoteValue args, int idx) 
        throws AgentRemoteException {
        
        FileData fileData = new FileData(getFileName(args, idx),
                                         getFileSize(args, idx),
                                         getWriteType(args, idx));

        String md5sum = getMD5CheckSum(args, idx);

        if (md5sum != null) {
            fileData.setMD5CheckSum(md5sum);
        }
        
        return fileData;
    }

    private int getNumFiles(AgentRemoteValue val)
        throws AgentRemoteException
    {
        return val.getValueAsInt(PROP_NFILES);
    }

    private void setNumFiles(int numFiles){
        this.setValue(PROP_NFILES, Integer.toString(numFiles));
    }
    
    private static String getFileName(AgentRemoteValue val, int idx)
        throws AgentRemoteException
    {
        return val.getValue(PROP_FILENAME + idx);
    }

    private void setFileName(int idx, String filename){
        this.setValue(PROP_FILENAME + idx, filename);
    }

    private static long getFileSize(AgentRemoteValue val, int idx)
        throws AgentRemoteException
    {
        return val.getValueAsLong(PROP_FILESIZE + idx);
    }

    private void setFileSize(int idx, long size){
        this.setValue(PROP_FILESIZE + idx, Long.toString(size));
    }

    private static int getWriteType(AgentRemoteValue val, int idx)
        throws AgentRemoteException
    {
        return val.getValueAsInt(PROP_WRITETYPE + idx);
    }

    private void setWriteType(int idx, int type){
        this.setValue(PROP_WRITETYPE + idx, Integer.toString(type));
    }
    
    private static String getMD5CheckSum(AgentRemoteValue val, int idx) {
        return val.getValue(PROP_MD5SUM + idx);
    }
    
    private void setMD5CheckSum(int idx, String md5sum) {
        this.setValue(PROP_MD5SUM + idx, md5sum);
    }
    
}
