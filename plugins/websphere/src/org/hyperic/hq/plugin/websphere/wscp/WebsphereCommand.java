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

package org.hyperic.hq.plugin.websphere.wscp;

import java.util.ArrayList;
import java.util.Properties;

/**
 * Wrapper around goofy wscp Tcl syntax, such as:
 * {/Node:thresher/ApplicationServer:Default Server/}
 */
public abstract class WebsphereCommand {

    private ArrayList names = new ArrayList();
    protected Properties props = new Properties();
    protected Properties metricProps = new Properties();
    private String fullname = null;

    public abstract String getObjectName();

    public WebsphereCommand() { }

    public WebsphereCommand(String value) {
        add(value);
    }

    public WebsphereCommand(WebsphereCommand cmd) {
        add(cmd);
    }

    public WebsphereCommand(WebsphereCommand cmd, String value) {
        add(cmd);
        add(value);
    }

    public String getObjectFullName() {
        return getObjectName();
    }

    public void add(String value) {
        this.names.add(getObjectName());
        this.names.add(value);
    }

    public void add(WebsphereCommand cmd) {
        this.names.addAll(cmd.getNames());
        this.props.putAll(cmd.getProperties());
    }

    public ArrayList getNames() {
        return this.names;
    }

    public Properties getProperties() {
        return this.props;
    }

    public Properties getMetricProperties() {
        return this.metricProps;
    }

    public String getLeafName() {
        return (String)this.names.get(this.names.size()-1);
    }

    public String getFullName() {
        if (this.fullname == null) {
            StringBuffer sb = new StringBuffer("/");

            for (int i=0; i<this.names.size(); i+=2) {
                sb.append((String)this.names.get(i));
                sb.append(":");
                sb.append((String)this.names.get(i+1));
                sb.append("/");
            }

            this.fullname = sb.toString();
        }

        return this.fullname;
    }

    public boolean hasControl() {
        return true;
    }

    public String toString() {
        return "{" + getFullName() + "}";
    }

    public boolean equals(Object o) {
        WebsphereCommand cmd = (WebsphereCommand)o;

        ArrayList cNames = cmd.getNames();
        if (cNames.size() != this.names.size()) {
            return false;
        }

        for (int i=0; i<cNames.size(); i++) {
            if (!cNames.get(i).equals(this.names.get(i))) {
                return false;
            }
        }

        return true;
    }
}
