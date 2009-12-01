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

package org.hyperic.hq.plugin.xen;

import java.util.Properties;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.VM;

public class XenVmControlPlugin extends ControlPlugin {

    private String _uuid;
    private Properties _props;

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        _props = config.toProperties();
        _uuid = _props.getProperty(XenUtil.PROP_SERVER_UUID);
    }

    private VM getVM(Connection conn) throws Exception {
        return VM.getByUuid(conn, _uuid);
    }

    private Connection connect() throws PluginException {
        return XenUtil.connect(_props);
    }

    public void doAction(String action, String[] args) throws PluginException {
        if (action.equals("shutdown")) {
            //avoiding super.shutdown override
            action = "stop";
        }
        super.doAction(action, args);
    }

    //Note: PluginException thrown with e.toString() rather than e.getMessage()
    //The majority of xenapi exceptions thrown will have an empty getMessage()
    //but implement toString() with the useful message

    public void start() throws PluginException {
        Connection conn = connect();
        try {
            VM vm = getVM(conn);
            vm.start(conn, false, false);
        } catch (Exception e) {
            throw new PluginException(e.toString(), e);
        }
    }

    //"shutdown"
    public void stop() throws PluginException {
        Connection conn = connect();
        try {
            VM vm = getVM(conn);
            vm.cleanShutdown(conn);
        } catch (Exception e) {
            throw new PluginException(e.toString(), e);
        }
    }

    public void forceShutdown() throws PluginException {
        Connection conn = connect();
        try {
            VM vm = getVM(conn);
            vm.hardShutdown(conn);
        } catch (Exception e) {
            throw new PluginException(e.toString(), e);
        }
    }

    public void reboot() throws PluginException {
        Connection conn = connect();
        try {
            VM vm = getVM(conn);
            vm.cleanReboot(conn);
        } catch (Exception e) {
            throw new PluginException(e.toString(), e);
        }
    }

    public void forceReboot() throws PluginException {
        Connection conn = connect();
        try {
            VM vm = getVM(conn);
            vm.hardReboot(conn);
        } catch (Exception e) {
            throw new PluginException(e.toString(), e);
        }
    }

    public void resume() throws PluginException {
        Connection conn = connect();
        try {
            VM vm = getVM(conn);
            vm.resume(conn, false, false);
        } catch (Exception e) {
            throw new PluginException(e.toString(), e);
        }
    }

    public void suspend() throws PluginException {
        Connection conn = connect();
        try {
            VM vm = getVM(conn);
            vm.suspend(conn);
        } catch (Exception e) {
            throw new PluginException(e.toString(), e);
        }
    }
}
