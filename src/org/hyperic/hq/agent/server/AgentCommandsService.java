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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentUpgradeManager;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.transport.util.RemoteInputStream;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.file.FileWriter;
import org.hyperic.util.math.MathUtil;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * The Agent Commands service.
 */
public class AgentCommandsService implements AgentCommandsClient {
    
    private static final Log _log = LogFactory.getLog(AgentCommandsService.class);

    private final AgentDaemon _agent;
    private final AgentTransportLifecycle _agentTransportLifecycle;
    
    public AgentCommandsService(AgentDaemon agent) throws AgentRunningException {
        _agent = agent;
        _agentTransportLifecycle = _agent.getAgentTransportLifecycle();
    }
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#agentSendFileData(org.hyperic.hq.agent.FileData[], java.io.InputStream[])
     */
    public FileDataResult[] agentSendFileData(FileData[] destFiles,
                                              InputStream[] streams)
            throws AgentRemoteException {
        
        // On the client side the streams representing each destination file 
        // are written to a single stream which we see here.
        if (streams.length != 1) {
            throw new AgentRemoteException("streams array should only contain one input stream");
        }
        
        RemoteInputStream inStream = (RemoteInputStream)streams[0];
        
        try {
            inStream.setRemoteSourceInvokerLocator(
                    _agentTransportLifecycle.getRemoteTransportLocator());
        } catch (Exception e) {
            throw new AgentRemoteException("failed to set the remote source invoker locator", e);
        }
        
        readFilesFromStream(destFiles, inStream);

        return new FileDataResult[0];
    }
    
    void agentSendFileData(AgentReceiveFileData_args args, InputStream inStream) 
        throws AgentRemoteException {
        
        int numFiles = args.getNumFiles();
        FileData[] destFiles = new FileData[numFiles];
        
        for (int i = 0; i < numFiles; i++) {
            destFiles[i] = args.getFile(i);
        }
        
        readFilesFromStream(destFiles, inStream);        
    }
    
    private void readFilesFromStream(FileData[] destFiles, InputStream inStream) 
        throws AgentRemoteException {

        List fList = new ArrayList();
        String errorMessage = null;

        int i;

        for (i = 0; i < destFiles.length; i++) {
            FileWriter writer;
            FileData data = destFiles[i];

            _log.info("Preparing to write " + data.getSize() +
                    " bytes to " + data.getDestFile() +
                    " (type=" + data.getWriteType() + ")");
            writer = new FileWriter(new File(data.getDestFile()), 
                    inStream, data.getSize());

            writer.setVerifyMD5CheckSumOnWrite(data.getMD5CheckSum());

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
        boolean checkSumFailed = false;

        for (i = 0; i < fList.size(); i++) {
            FileWriter writer = (FileWriter)fList.get(i);

            try {
                _log.info("Writing to '" + 
                        writer.getDestFile().getAbsolutePath() + "'");
                writer.write();

                try {
                    writer.verifyMD5CheckSum();                    
                } catch (IOException e) {
                    checkSumFailed = true;
                    throw e;
                }
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
            // if the failure occurred because of the md5 check sum, we don't 
            // need to chomp the first stream
            if (checkSumFailed && j==i) {
                continue;
            }

            FileData data = destFiles[j];

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
        AgentUpgradeManager.restartJVM();
    }
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#getCurrentAgentBundle()
     */
    public String getCurrentAgentBundle() throws AgentRemoteException {
        return _agent.getCurrentAgentBundle();
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#upgrade(java.lang.String, java.lang.String)
     */
    public void upgrade(String bundle, String destination)
            throws AgentRemoteException {
        final File bundleFile = new File(bundle);
        final File workDir = new File(destination, "work");
        try {
            _log.info("Preparing to upgrade agent bundle from file " + bundle +
                    " at destination " + destination);
            // check that we are running in Java Service Wrapper mode
            if (!WrapperManager.isControlledByNativeWrapper()) {
                throw new AgentRemoteException(
                        "Upgrade command is not supported without the Java Service Wrapper.");
            }
            
            // check that the bundle file exists and is a file
            // we are assuming at this point that the file is not corrupted
            if (!bundleFile.isFile()) {
                throw new AgentRemoteException("Upgrade agent bundle "
                        + bundle + " is not a valid file");
            }

            // assume that the bundle name is the same as the top level directory
            final String bundleHome = getBundleHome(bundleFile);

            // check if the bundle home directory exists
            final File bundleDir = new File(destination, bundleHome);
            if (bundleDir.exists()) {
                throw new AgentRemoteException("Bundle directory "
                        + bundleDir.toString() + " already exists");
            }

            // delete work directory in case it wasn't cleaned up
            FileUtil.deleteDir(workDir);
            // extract to work directory
            try {
                FileUtil.decompress(bundleFile, workDir);
            }
            catch (IOException e) {
                _log.error("Failed to decompress " + bundle + " at destination " + workDir, e);
                throw new AgentRemoteException(
                        "Failed to decompress " + bundle + " at destination " + workDir);
            }

            // update the wrapper configuration for next JVM restart
            boolean success = false;
            try {
                success = AgentUpgradeManager.upgrade(bundleHome);
            }
            catch (IOException e) {
                _log.error("Failed to write new bundle home " + bundleHome
                        + " into rollback properties", e);
            }
            finally {
                if (!success) {
                    throw new AgentRemoteException(
                            "Failed to write new bundle home " + bundleHome
                            + " into rollback properties");
                }
            }

            final File extractedBundleDir = new File(workDir,  bundleHome);
            // verify that top level dir exists
            if (!extractedBundleDir.isDirectory()) {
                throw new AgentRemoteException(
                        "Invalid agent bundle file detected; missing top-level "
                                + bundleDir + " directory");
            }
         // if everything went well, move extracted files to destination
            if (!extractedBundleDir.renameTo(bundleDir)) {
                throw new AgentRemoteException(
                        "Failed to copy agent bundle from " + extractedBundleDir + " to " + bundleDir);
            }
            _log.info("Successfully upgraded to new agent bundle");
        }
        // cleanup work dir files and bundle
        finally {
            doUpgradeCleanup(bundleFile, workDir);
        }
    }

    private void doUpgradeCleanup(File bundleFile, File workDir) {
        bundleFile.delete();
        // recursive delete
        FileUtil.deleteDir(workDir);
    }

    private String getBundleHome(File bundleFile)
            throws AgentRemoteException {
        final int index;
        String fileName = bundleFile.getName();
        if (fileName.endsWith(".tar.gz")) {
            index = fileName.lastIndexOf(".tar.gz");
        }
        else if (fileName.endsWith(".tgz")) {
            index = fileName.lastIndexOf(".tgz");
        }
        else if (fileName.endsWith(".zip")) {
            index = fileName.lastIndexOf(".zip");
        }
        else {
            throw new AgentRemoteException(
                    "Invalid file format for the agent bundle tar file (.zip, .tar.gz or .tgz expected)");
        }
        return fileName.substring(0, index);
    }

}
