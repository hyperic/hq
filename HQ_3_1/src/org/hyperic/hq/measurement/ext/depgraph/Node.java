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

import java.util.LinkedList;
import java.util.Set;

import org.hyperic.hq.measurement.server.session.MeasurementTemplate;

/**
 * Interface representing a node in a derived measurement dependency graph.
 */
public interface Node {
    /**
     * Interval representing a cyclic graph.
     */
    public static final int CYCLE = -1;

    /**
     * Constant for nodes that have not yet been visited during graph
     * traversal.
     */
    public static final int NOT_VISITED = 0;

    /**
     * Constant for nodes that are in the process of being visited
     * during graph traversal.
     */
    public static final int VISITING = 1;

    /**
     * Constant for nodes that have already been visited during graph
     * traversal.
     */
    public static final int VISITED = 2;

    /**
     * Return the id of this node.
     */
    public int getId();

    /**
     * Return the visitation state of this node during a graph
     * traversal.
     */
    public int getVisited();

    /**
     * Set the visitation state of this node.
     */
    public void setVisited(int visited);

    /**
     * Add an incoming edge to this node.
     *
     * @param node the node who feeds into this node
     */
    public void addIncoming(Node node);

    /**
     * Add an outgoing edge to this node.
     *
     * @param node the node who this node feeds into
     */
    public void addOutgoing(Node node) throws InvalidGraphException;

    /**
     * Return all of the nodes with edges coming into this node.
     */
    public Set getIncoming();

    /**
     * Return all of the nodes to which this node has outgoing edges.
     */
    public Set getOutgoing();

    /**
     * Push outgoing nodes onto the given stack, depth-first.
     *
     * @param stack the current stack of dependent nodes
     */
    public void pushOutgoing(LinkedList stack);

    /**
     * Get the measurement template for this node.
     */
    public MeasurementTemplate getMeasurementTemplate();
}


