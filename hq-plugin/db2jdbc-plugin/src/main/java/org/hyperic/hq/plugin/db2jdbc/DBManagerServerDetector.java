/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.db2jdbc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author laullon
 */
public class DBManagerServerDetector extends DefaultServerDetector {

    Pattern regExpInstall = Pattern.compile("([^ ]*) *(\\d*\\.\\d*\\.\\d*\\.\\d*) *([^ ]*)");

    protected List createServers(String installPath) {
        ServerResource server = new ServerResource();
        server.setType(getTypeInfo().getName());
        //server.setName(getPlatformName() + " " + getTypeInfo().getName() + " " + installPath);
        server.setName(getPlatformName() + " DB2DM " + installPath);
        server.setInstallPath(installPath);
        server.setIdentifier(server.getName());

        ConfigResponse sc = new ConfigResponse();
        setProductConfig(server, sc);

        List res = new ArrayList();
        res.add(server);
        return res;
    }
}
