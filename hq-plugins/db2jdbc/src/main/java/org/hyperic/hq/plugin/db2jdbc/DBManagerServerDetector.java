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
