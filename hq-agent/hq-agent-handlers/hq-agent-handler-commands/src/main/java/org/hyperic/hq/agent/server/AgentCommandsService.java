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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentUpgradeManager;
import org.hyperic.hq.agent.FileData;
import org.hyperic.hq.agent.FileDataResult;
import org.hyperic.hq.agent.client.AgentCommandsClient;
import org.hyperic.hq.agent.commands.AgentReceiveFileData_args;
import org.hyperic.hq.agent.commands.AgentUpgrade_result;
import org.hyperic.hq.transport.util.RemoteInputStream;
import org.hyperic.util.JDK;
import org.hyperic.util.StringUtil;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.file.FileWriter;
import org.hyperic.util.math.MathUtil;
import org.tanukisoftware.wrapper.WrapperManager;

/**
 * The Agent Commands service.
 */
public class AgentCommandsService implements AgentCommandsClient {
    
    private static final String AGENT_BUNDLE_HOME = AgentConfig.AGENT_BUNDLE_HOME;

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
    
    // replaces tokenized agent.bundle.home property from path
    private String resolveAgentBundleHomePath(String path) throws AgentRemoteException {
        String agentBundleHome = System.getProperty(AGENT_BUNDLE_HOME);
        // this should never happen
        if (agentBundleHome == null) {
                throw new AgentRemoteException(
                    "Could not resolve system property " + AGENT_BUNDLE_HOME);
        }
        return StringUtil.replace(path, "${" + AGENT_BUNDLE_HOME + "}", agentBundleHome);
    }
    
    private void readFilesFromStream(FileData[] destFiles, InputStream inStream) 
        throws AgentRemoteException {

        List fList = new ArrayList();
        String errorMessage = null;

        int i;

        for (i = 0; i < destFiles.length; i++) {
            FileWriter writer;
            FileData data = destFiles[i];

            // replace agent bundle home environment property from path
            String destFile = resolveAgentBundleHomePath(data.getDestFile());

            _log.info("Preparing to write " + data.getSize() +
                    " bytes to " + destFile +
                    " (type=" + data.getWriteType() + ")");
            writer = new FileWriter(new File(destFile), 
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
            if (checkSumFailed && (j==i)) {
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

    private void setExecuteBit(File file) throws AgentRemoteException {
        int timeout = 10 * 6000;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecuteWatchdog watch = new ExecuteWatchdog(timeout);
        Execute exec = new Execute(new PumpStreamHandler(output), watch);
        int rc;

        try {
            String[] arguments = {"chmod", "+x", file.getCanonicalPath()};
            exec.setCommandline(arguments);

            _log.info("Running " + exec.getCommandLineString());
            rc = exec.execute();
        } catch (Exception e) {
             rc = -1;
             _log.error(e);
        }

        if (rc != 0) {
            String msg = output.toString().trim();
            if (msg.length() == 0) {
                msg = "timeout after " + timeout + "ms";
            }
            throw new AgentRemoteException("Failed to set permissions: " + "[" +
                                           exec.getCommandLineString() + "] " +
                                           msg);
        }
    }

    /**
     * @see org.hyperic.hq.agent.client.AgentCommandsClient#upgrade(java.lang.String, java.lang.String)
     */
    public Map upgrade(String bundle, String destination)
    throws AgentRemoteException {
        final File bundleFile = new File(bundle);
        final File workDir = new File(destination, "work");
        
        Map result = new HashMap();
        
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
            String bundleHome = getBundleHome(bundleFile);

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

            // check if the bundle home directory exists
            File bundleDir = new File(destination, bundleHome);

            final File extractedBundleDir = new File(workDir,  bundleHome);
            // verify that top level dir exists
            if (!extractedBundleDir.isDirectory()) {
                throw new AgentRemoteException(
                        "Invalid agent bundle file detected; missing top-level "
                                + bundleDir + " directory");
            }
            
            if (bundleDir.exists()) {
            	 // TODO HQ-2428 Since we use maven and no longer have build numbers, there needs to be a way to differentiate between snapshot builds.  After some discussion,
                //      we decided to ensure bundle folder name uniqueness by timestamp.  This means that users could "upgrade" to the same version, HQ will no longer prevent
                //      this scenario...
            	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            	
            	bundleHome += "-" + dateFormat.format(new Date(System.currentTimeMillis()));
            	bundleDir = new File(destination, bundleHome);
            }

            // if everything went well, move extracted files to destination
            if (!extractedBundleDir.renameTo(bundleDir)) {
                throw new AgentRemoteException(
                        "Failed to copy agent bundle from " + extractedBundleDir + " to " + bundleDir);
            }

            // Handle potential permissions issues
            if (!JDK.IS_WIN32) {
                File pdkdir = new File(bundleDir, "pdk");
                File pdklibdir = new File(pdkdir, "lib");
                File jredir = new File(pdkdir, "jre");
                if (!pdklibdir.exists()) {
                    throw new AgentRemoteException("Invalid PDK library directory " +
                            pdklibdir.getAbsolutePath());
                }

                File[] libs = pdklibdir.listFiles();
                for (File lib : libs) {
                    if (lib.getName().endsWith("sl")) {
                        // chmod +x ./bundles/$AGENT_BUNDLE/pdk/lib/*.sl
                        setExecuteBit(lib);
                    }
                }

                if (jredir.exists()) {
                    File jrebin = new File(jredir, "bin");
                    File[] bins = jrebin.listFiles();
                    // chmod +x ./bundles/$AGENT_BUNDLE/jre/bin/*
                    for (File bin : bins) {
                        setExecuteBit(bin);
                    }
                }

                File pdkscriptsdir = new File(pdkdir, "scripts");
                if (!pdkscriptsdir.exists()) {
                    throw new AgentRemoteException("Invalid PDK scripts directory " +
                            pdklibdir.getAbsolutePath());
                }

                File[] scripts = pdkscriptsdir.listFiles();
                for (File script : scripts) {
                    // chmod +x ./bundles/$AGENT_BUNDLE/pdk/scripts/*
                    setExecuteBit(script);
                }
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
            _log.info("Successfully upgraded to new agent bundle");
            
            try {
                _log.debug("Creating result map");
                
                // Grab the hq-version.properties file from the new hq bundle
                final File versionFile = new File(bundleDir,"lib/hq-version.properties");
                FileInputStream versionInputStream = new FileInputStream(versionFile);
                
                Properties newVersionProperties = new Properties();
                newVersionProperties.load(versionInputStream);

                // Created return map
                String version = newVersionProperties.getProperty("version");
               
                _log.debug("VERSION: " + version);
                _log.debug("BUNDLE_NAME: " + bundleHome);
                
                result.put(AgentUpgrade_result.VERSION, version);
                result.put(AgentUpgrade_result.BUNDLE_NAME, bundleHome);
            } catch(MalformedURLException e) {
                _log.warn("Could not access new hq-version.properties due to a malformed url, version value will not be updated in the database.", e);
            } catch(IOException e) {
                _log.warn("Could not read new hq-version.properties file, version value will not be updated in the database.", e);
            }
        }
        // cleanup work dir files and bundle
        finally {
            doUpgradeCleanup(bundleFile, workDir);
        }
        
        return result;
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

    public Map<String, Boolean> agentRemoveFile(Collection<String> files) {
        final Map<String, Boolean> rtn = new HashMap<String, Boolean>();
        final boolean debug = _log.isDebugEnabled();
        for (final String filename : files) {
            if (filename == null) {
                continue;
            }
            try {
                final File file = new File(resolveAgentBundleHomePath(filename));
                file.createNewFile();
                if (debug) {
                    _log.debug("removing file=" + file.getAbsolutePath());
                }
                rtn.put(filename, file.exists());
            } catch (Exception e) {
                _log.warn("could not remove file " + filename + ": " + e);
                _log.debug(e,e);
                rtn.put(filename, false);
            }
        }
        return rtn;
    }

}
