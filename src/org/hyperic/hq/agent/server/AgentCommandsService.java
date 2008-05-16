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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
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
    
    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#getCurrentAgentBundle()
     */
    public String getCurrentAgentBundle() throws AgentRemoteException {
        String agentBundleHome = _agent.getBootConfig()
              .getBootProperties().getProperty(AgentConfig.PROP_BUNDLEHOME[0]);
                
        File bundleDir = new File(agentBundleHome);
        return bundleDir.getName();
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#upgrade(java.lang.String, java.lang.String)
     */
    public void upgrade(String tarball, String destination)
            throws AgentRemoteException {
        final File tarFile = new File(tarball);
        final File workDir = new File(destination, "work");
        try {
            _log.info("Preparing to upgrade agent bundle from tarball " + tarball +
                    " at destination " + destination);
            // check that we are running in Java Service Wrapper mode
            if (!WrapperManager.isControlledByNativeWrapper()) {
                throw new AgentRemoteException(
                        "Upgrade command is not supported without the Java Service Wrapper.");
            }
            
            // check that the tar file exists and is a file
            // we are assuming at this point that the file is not corrupted
            if (!tarFile.isFile()) {
                throw new AgentRemoteException("Upgrade agent bundle "
                        + tarball + " is not a valid file");
            }

            // assume that the tarball name is the same as the top level directory
            final String bundleHome = getBundleHome(tarFile);

            // check if the bundle home directory exists
            final File bundleDir = new File(destination, bundleHome);
            if (bundleDir.exists()) {
                throw new AgentRemoteException("Bundle directory "
                        + bundleDir.toString() + " already exists");
            }

            // delete work directory in case it wasn't cleaned up
            FileUtil.deleteDir(workDir);
            // untar to work directory
            try {
                FileUtil.untar(tarFile, workDir);
            }
            catch (IOException e) {
                _log.error("Failed to untar " + tarball + " at destination " + workDir, e);
                throw new AgentRemoteException(
                        "Failed to untar " + tarball + " at destination " + workDir);
            }

            // update the wrapper configuration for next JVM restart
            boolean success = false;
            try {
                success = writeRollbackProperties(bundleHome);
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
                        "Invalid agent bundle tar file detected; missing top-level "
                                + bundleDir + " directory");
            }
         // if everything went well, move extracted files to destination
            if (!extractedBundleDir.renameTo(bundleDir)) {
                throw new AgentRemoteException(
                        "Failed to copy agent bundle from " + extractedBundleDir + " to " + bundleDir);
            }
            _log.info("Successfully upgraded to new agent bundle");
        }
        // cleanup work dir files and tarball
        finally {
            doUpgradeCleanup(tarFile, workDir);
        }
    }

    private void doUpgradeCleanup(File tarFile, File workDir) {
        tarFile.delete();
        // recursive delete
        FileUtil.deleteDir(workDir);
    }

    private String getBundleHome(File tarFile)
            throws AgentRemoteException {
        final int index;
        String tarFileName = tarFile.getName();
        if (tarFileName.endsWith(".tar.gz"))
            index = tarFileName.lastIndexOf(".tar.gz");
        else if (tarFileName.endsWith(".tgz"))
            index = tarFileName.lastIndexOf(".tgz");
        else
            throw new AgentRemoteException(
                    "Invalid file format for the agent bundle tar file (.tar.gz or .tgz expected)");
        return tarFileName.substring(0, index);
    }

    // replaces the old bundle home with the new one
    // copying over the old one as rollback in the rollback properties
    // file used by Java Server Wrapper
    private boolean writeRollbackProperties(String bundleDir) throws IOException {

        Properties rollbackProps = new Properties();
        FileInputStream fis = null;
        String propFileName = System.getProperty(AgentConfig.ROLLBACK_PROPFILE,
                AgentConfig.DEFAULT_ROLLBACKPROPFILE);
        File propFile = new File(propFileName);
        File tempPropFile = new File(propFileName + ".tmp");
        try {
            fis = new FileInputStream(propFile);
            rollbackProps.load(fis);
            String oldBundleDir = rollbackProps
                    .getProperty(AgentConfig.JSW_PROP_AGENT_BUNDLE);
            rollbackProps.setProperty(AgentConfig.JSW_PROP_AGENT_BUNDLE,
                    bundleDir);
            rollbackProps.setProperty(
                    AgentConfig.JSW_PROP_AGENT_ROLLBACK_BUNDLE, oldBundleDir);
        }
        catch (IOException e) {
            _log.error("Failed to load rollback properties file", e);
            throw e;
        }
        finally {
            FileUtil.safeCloseStream(fis);
        }
        // write out the updated rollback properties
        FileOutputStream fos = null;
        try {
            try {
                tempPropFile.delete();
                fos = new FileOutputStream(tempPropFile);
                rollbackProps.store(fos,
                        "Auto-generated rollback properties do not edit!");
            }
            catch (IOException e) {
                _log.error("Failed to write rollback properties file", e);
                throw e;
            }
            finally {
                FileUtil.safeCloseStream(fos);
            }
            return FileUtil.safeFileMove(tempPropFile, propFile);
        }
        finally {
            tempPropFile.delete();
        }
    }

}
