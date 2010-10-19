/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.core;

import org.springframework.context.support.AbstractApplicationContext;
 
import java.util.List;

/**
 * RabbitNode
 * @author Helena Edelson
 */
public class RabbitNode {

    private long pid;

    private String name;

    private String[] processArgs;

    private List<RabbitVirtualHost> virtualHosts;

    public RabbitNode(String name, long pid, String[] processArgs) {
        this.name = name;
        this.pid = pid;
        this.processArgs = processArgs;

    }
 
    public List<RabbitVirtualHost> getVirtualHosts() {
        return virtualHosts;
    }

    public void setVirtualHosts(List<RabbitVirtualHost> virtualHosts) {
        this.virtualHosts = virtualHosts;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getProcessArgs() {
        return processArgs;
    }

    public void setProcessArgs(String[] processArgs) {
        this.processArgs = processArgs;
    }
}
