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

import com.jcraft.jsch.UserInfo;

public class SSHUserInfo implements UserInfo {
    String passwd;
    
    public SSHUserInfo(String passwd) {
        super();
        this.passwd = passwd;
    }

    public boolean promptYesNo(String str){
        //System.out.println(str);
        try {
            //XXX: Required to allow for handshake
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        
        return true;
    }
    
    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ 
        return true; 
    }

    public String getPassword(){ 
        return passwd; 
    }
    public boolean promptPassword(String message){
        try {
            //XXX: Required to allow for handshake
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        return true;
    }
    
    public void showMessage(String message){
        //System.out.println(message);
    }
    
    public boolean getTrust() {
        return true;
    }

    public boolean retry() {
        return false;
    }
}

