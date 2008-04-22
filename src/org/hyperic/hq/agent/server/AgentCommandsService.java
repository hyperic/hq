/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.agent.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.util.file.FileWriter;
import org.hyperic.util.math.MathUtil;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * The Agent Commands service.
 */
public class AgentCommandsService implements AgentCommandsClient {
    
    private static final Log _log = LogFactory.getLog(AgentCommandsService.class);

    private final AgentDaemon _agent;
    
    public AgentCommandsService(AgentDaemon agent) {
        _agent = agent;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#agentSendFileData(org.hyperic.hq.appdef.shared.AppdefEntityID, org.hyperic.hq.agent.FileData[], java.io.InputStream[])
     */
    public FileDataResult[] agentSendFileData(AppdefEntityID id,
                                              FileData[] destFiles, 
                                              InputStream[] streams)
            throws AgentRemoteException {
        
        // TODO need to support file transfer
        throw new UnsupportedOperationException("file transfer not supported");    
    }
    
    void agentSendFileData(AgentReceiveFileData_args args,
                           InputStream inStream) 
        throws AgentRemoteException {
        
        ArrayList fList = new ArrayList();
        String errorMessage = null;
        int nFiles, i;

        nFiles = args.getNumFiles();

        for (i = 0; i < nFiles; i++) {
            FileWriter writer;
            FileData data;

            data = args.getFile(i);

            _log.info("Preparing to write " + data.getSize() +
                    " bytes to " + data.getDestFile() +
                    " (type=" + data.getWriteType() + ")");
            writer = new FileWriter(new File(data.getDestFile()), 
                    inStream, data.getSize());
            switch(data.getWriteType()){
            case FileData.WRITETYPE_CREATEONLY:
                writer.setCreateOnly();
                break;
            case FileData.WRITETYPE_CREATEOROVERWRITE:
                writer.setCreateOrOverwrite();
                break;
            case FileData.WRITETYPE_REWRITE:
                writer.setRewrite();
                break;
            default:
                throw new AgentRemoteException("Unknown control write type: " +
                        data.getWriteType());
            }

            fList.add(writer);
        }

//      Now do the actual writing
        for (i = 0; i < fList.size(); i++) {
            FileWriter writer = (FileWriter)fList.get(i);

            try {
                _log.info("Writing to '" + 
                        writer.getDestFile().getAbsolutePath() + "'");
                writer.write();
            } catch(IOException exc) {
                errorMessage ="Error writing to '" + 
                writer.getDestFile().getAbsolutePath() + "': " +
                exc.getMessage();

                _log.error(errorMessage, exc);
                break;
            }
        }

//      Make sure the streams are synchronized by chomping off all the
//      data from the input stream that we would have eaten, had the 
//      operation succeeded
        for(int j=i; j<fList.size(); j++){
            FileData data = args.getFile(j);

            _log.debug("Resynching stream:  Reading " + data.getSize() +" bytes");
            try {
                byteChomper(inStream, data.getSize());
            } catch(IOException exc){
                _log.error("Error occurred while chomping stream: " +
                        exc.getMessage());
            }
        }

        if (errorMessage != null) {
//          'i' is the last writer we tried to write.  Go from
//          that, back to 0, rolling back 
            i++;  
            while (i-- != 0) {
                FileWriter writer = (FileWriter)fList.get(i);
                String destFile = writer.getDestFile().getAbsolutePath();

                _log.info("Rolling back '" + destFile + "'");
                try {
                    writer.rollback();
                } catch(IOException exc){
                    _log.error("Error rolling back '" + destFile + 
                            ": " + exc.getMessage());
                }
            }
            throw new AgentRemoteException(errorMessage);
        }

//      Everything succeeded -- cleanup, and registger backup files
        for (i = 0; i < fList.size(); i++) {
            FileWriter writer = (FileWriter)fList.get(i);
            writer.cleanup();
            String destFile = writer.getDestFile().getAbsolutePath();

            _log.info("Successfully wrote: " + destFile);
        }
    }

    /**
     * Read a certain # of bytes from a stream, throwing all the
     * data away.
     *
     * @param inStream Stream to read from
     * @param nBytes   Number of bytes to read & throw away
     */
    private void byteChomper(InputStream inStream, long nBytes) throws IOException {
        byte[] buf = new byte[8192];

        while(nBytes != 0){
            int nToRead, nRead;

//          Safe to truncate to int here, since buf.length is an int
            nToRead = (int)MathUtil.clamp(nBytes, 1, buf.length);
            if((nRead = inStream.read(buf, 0, nToRead)) == -1){
                throw new IOException("Unable to chomp " + nBytes + 
                " EOF reached");
            }

            nBytes -= nRead;
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#die()
     */
    public void die() throws AgentRemoteException {
        try {
            _agent.die();
        } catch(AgentRunningException exc){
            // This should really never happen
            _log.error("Killing a running agent!");
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#ping()
     */
    public long ping() throws AgentRemoteException {
        return 0;
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#restart()
     */
    public void restart() throws AgentRemoteException {
        Timer t = new Timer();
        t.schedule(new TimerTask()  { 
            public void run() {
                WrapperManager.restart();
            }}  , 0);
    }

}
