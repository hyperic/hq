/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
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

package org.hyperic.hq.amqp.admin.erlang;

import java.io.Serializable;
import java.util.List;

/**
 * @author Helena Edelson
 */
@SuppressWarnings("serial")
public class BrokerStatus implements Serializable {

	private List<Node> nodes;

	private List<Node> runningNodes;

	public BrokerStatus(List<Node> nodes, List<Node> runningNodes) {
		this.runningNodes = runningNodes;
	}

	public boolean isAlive() {
		return !nodes.isEmpty();
	}

	public boolean isRunning() {
		return !runningNodes.isEmpty();
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public List<Node> getRunningNodes() {
		return runningNodes;
	}

    @Override
	public String toString() {
		return "RabbitStatus [runningNodes=" + runningNodes + ", nodes=" + nodes + "]";
	}
}
