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

package org.hyperic.util.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSchException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.hyperic.util.TextProgressBar;

public class SSHCopy extends SSHRemoteExec {
    
    public SSHCopy(String user, String password, String host) {
        super(user, password, host);       
    }

    public void delete(String file) 
        throws SSHRemoteException, SSHExecException {
        this.execute("rm -f " + file);
    }
    
    public void mkDir(String path) 
        throws SSHRemoteException, SSHExecException {
        this.execute("mkdir -p " + path); 
    }

    public void copy(File file, String remotePath) 
        throws SSHRemoteException
    {
        copy(file, remotePath, false);
    }

    public void copy(File file, String remotePath,
                     boolean showProgress)
        throws SSHRemoteException
    {
        Session session = null;
        Channel channel = null;
        try {
            session = openSession();
            channel = session.openChannel("exec");
            String command = "scp -p -t " + remotePath;
            ((ChannelExec)channel).setCommand(command);

            InputStream in = channel.getInputStream();
            OutputStream out = channel.getOutputStream();
            channel.connect();
        
            waitForAck(in);

            doSingleCopy(file, in, out, showProgress);

        } catch (JSchException e) {
            throw new SSHRemoteException("Error connecting to host: " + e, e);
        } catch (IOException e) {
            throw new SSHRemoteException("I/O error: " + e, e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    public void doSingleCopy(File file, InputStream in,
                             OutputStream out, boolean showProgress) 
        throws SSHRemoteException
    {
        try {
            String command = "C0644 " + file.length() + " " +
                file.getName() + "\n";
            out.write(command.getBytes()); out.flush();
            waitForAck(in);
        
            // Send the file
            FileInputStream fis = new FileInputStream(file);
            TextProgressBar progress = new TextProgressBar(System.out,
                                                           file.getName(),
                                                           file.length());
            int sent = 0;
            byte[] buf = new byte[BUFFER_SIZE];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) 
                    break;
                out.write(buf, 0, len);
                out.flush();

                sent += len;
                if (showProgress) {
                    progress.print(sent);
                }
            }

            if (showProgress) {
                progress.print(sent);
            }
            
            // send '\0'
            sendAck(out);
            waitForAck(in);
        } catch (IOException e) {
            throw new SSHRemoteException("I/O error: " + e, e);
        }
    }
}
