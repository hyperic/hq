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
package org.hyperic.hq.plugin.websphere;

import java.io.File;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;

public class WebSphereProcess {

    String installRoot;
    String serverRoot;
    private String node;
    private String server;
    private String cell;
    private long pid;

    public WebSphereProcess() {
    }

    public WebSphereProcess(ConfigResponse config) {
        serverRoot = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        node = config.getValue(WebsphereProductPlugin.PROP_SERVER_NODE);
        cell = config.getValue(WebsphereProductPlugin.PROP_SERVER_CELL);
        server = config.getValue(WebsphereProductPlugin.PROP_SERVER_NAME);
    }

    public ConfigResponse getConfig() {
        ConfigResponse res = new ConfigResponse();
        res.setValue(ProductPlugin.PROP_INSTALLPATH, serverRoot);
        res.setValue(WebsphereProductPlugin.PROP_SERVER_NODE, node);
        res.setValue(WebsphereProductPlugin.PROP_SERVER_CELL, cell);
        res.setValue(WebsphereProductPlugin.PROP_SERVER_NAME, server);
        return res;
    }

    boolean isConfigured() {
        return (this.getInstallRoot() != null)
                && (this.getServerRoot() != null)
                && (this.getNode() != null)
                && (this.getServer() != null)
                && (this.getCell() != null);
    }

    boolean isPropsConfigured() {
        return (this.getInstallRoot() != null)
                && (this.getServerRoot() != null);
    }

    public String toString() {
        return "was.install.root=" + this.getInstallRoot()
                + ", server.root=" + this.getServerRoot()
                + ", server.server='" + this.getServer() + "'"
                + ", server.node='" + this.getNode() + "'"
                + ", server.cell='" + this.getCell() + "'";
    }

    /**
     * @return the installRoot
     */
    public String getInstallRoot() {
        return installRoot;
    }

    /**
     * @return the serverRoot
     */
    public String getServerRoot() {
        return serverRoot;
    }

    /**
     * @return the node
     */
    public String getNode() {
        return node;
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @return the cell
     */
    public String getCell() {
        return cell;
    }

    /**
     * @param installRoot the installRoot to set
     */
    public void setInstallRoot(String installRoot) {
        this.installRoot = installRoot;
    }

    /**
     * @param serverRoot the serverRoot to set
     */
    public void setServerRoot(String serverRoot) {
        this.serverRoot = serverRoot;
    }

    /**
     * @param node the node to set
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @param cell the cell to set
     */
    public void setCell(String cell) {
        this.cell = cell;
    }

    // XXX arreglar esto el AID debe de ser mas completo, pero viguilar la compativilidad para atras
    public String getIdentifier() {
        return getServerRoot() + " " + getServerName();
        //return getServerName();
    }

    // XXX arreglar esto el AID debe de ser mas completo, pero viguilar la compativilidad para atras
    public String getServerName() {
        String profile = getServerRoot().substring(getServerRoot().lastIndexOf(File.separator) + 1) + " ";
        return profile + getCell() + " " + getNode() + " " + getServer();
        //return node+" "+server;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }
}
