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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHRemoteExec extends SSHBase {

    int BUFFER_SIZE = 1024;

    protected String command;

    public SSHRemoteExec(String user, String password, String host) {
        super(user, password, host);
    }

    public void execute(String command)
    	throws SSHRemoteException, SSHExecException
    {
    	execute(command, System.out, System.err);
    }
    
    public void execute(String command, PrintStream os, PrintStream err) 
        throws SSHRemoteException, SSHExecException 
    {
        Session session = null;
        ChannelExec channel = null;
        try {
            session = openSession();
            channel = (ChannelExec)session.openChannel("exec");
            channel.setCommand(command);
            channel.setOutputStream(os);
            channel.setErrStream(err);

            InputStream in = channel.getInputStream();

            channel.connect();

            byte[] buf = new byte[BUFFER_SIZE];
            while (true) {
                while(in.available() > 0){
                    int i = in.read(buf, 0, BUFFER_SIZE);
                    if(i < 0)
                        break;
                    os.print(new String(buf, 0, i));
                }
                if (channel.isClosed()){
                    if (channel.getExitStatus() != 0) {
                        throw new SSHExecException("Error running command: " +
                                                   " Exit code: " + 
                                                   channel.getExitStatus(),
                                                   channel.getExitStatus());
                    }
                    break;
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
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

    protected void sendAck(OutputStream out) 
        throws IOException
    {
        byte[] buf = new byte[1];
        buf[0] = 0;
        out.write(buf);
        out.flush();
    }

    /**
     * Reads the response, throws a RemoteAccessException 
     * indicates an error.
     */
    protected void waitForAck(InputStream in) 
        throws SSHRemoteException, IOException
    {
        int b = in.read();

        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,

        if (b == -1) {
            // didn't receive any response
            throw new SSHRemoteException("No response from server");
        } else if (b != 0) {
            StringBuffer sb = new StringBuffer();

            int c = in.read();
            while (c > 0 && c != '\n') {
                sb.append((char) c);
                c = in.read();
            }

            if (b == 1) {
                throw new SSHRemoteException("Server indicated an error: " +
                                             sb.toString());
            } else if (b == 2) {
                throw new SSHRemoteException("Server indicated a fatal " +
                                             "error: " + sb.toString());
            } else {
                throw new SSHRemoteException("Unknown response, code " + b +
                                             " message: " + sb.toString());
            }
        }
    }

    public static void main(String args[]) 
        throws Exception
    {
        if (args.length != 4) {
            System.err.println("Usage: ip user password cmd");
            return;
        }

        SSHRemoteExec exec = new SSHRemoteExec(args[1], args[2], args[0]);
        exec.execute(args[3], System.out, System.err);
    }
}
