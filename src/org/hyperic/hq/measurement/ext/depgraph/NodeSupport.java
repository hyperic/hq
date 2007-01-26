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

package org.hyperic.hq.measurement.ext.depgraph;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.measurement.server.session.MeasurementTemplate;

public abstract class NodeSupport implements Node, java.io.Serializable {
    // data
    private int _id;
    private int _visited = NOT_VISITED;
    private Set _incoming = new HashSet();
    private Set _outgoing = new HashSet();
    private MeasurementTemplate _mt;

    public NodeSupport(int id, MeasurementTemplate mt) {
        _id = id;
        _mt = mt;
    }

    public int getId() {
        return _id;
    }

    public int getVisited() {
        return _visited;
    }

    public void setVisited(int visited) {
        _visited = visited;
    }

    public void addIncoming(Node node) {
        _incoming.add(node);
    }

    public void addOutgoing(Node node) throws InvalidGraphException {
        _outgoing.add(node);
    }

    public Set getIncoming() {
        return _incoming;
    }

    public Set getOutgoing() {
        return _outgoing;
    }

    public MeasurementTemplate getMeasurementTemplate() {
        return _mt;
    }
}

