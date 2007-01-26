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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.util.math.MathUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A derived node represents a derived measurement.  It can contain
 * outgoing nodes to either raw or derived nodes.
 *
 */
public class DerivedNode extends NodeSupport implements java.io.Serializable {
    protected static final Log log = LogFactory.getLog(DerivedNode.class);

    // if not set, getInterval() will use dependent interval, since
    // it's an LCM calculation
    private long _interval = 1;

    public DerivedNode(int id, MeasurementTemplate mt) {
        super(id, mt);
    }

    public DerivedNode(int id, long interval, MeasurementTemplate mt) {
        super(id, mt);
        _interval = interval;
    }

    public void pushOutgoing(LinkedList stack) {
        if (getVisited() != VISITED) {
            setVisited(VISITING);
            for (Iterator it=getOutgoing().iterator(); it.hasNext();) {
                Node node = (Node)it.next();
                node.pushOutgoing(stack);
            }
            stack.add(this);
            setVisited(VISITED);
        }
    }

    public void setInterval(long interval) {
        _interval = interval;
    }

    public long getInterval() throws CircularDependencyException {
        if (getVisited() != VISITED) {
            setVisited(VISITING); // mark node as "currently being visited"

            // visit the node
            for (Iterator it=getOutgoing().iterator(); it.hasNext();) {
                Node node = (Node)it.next();
                if (node.getVisited() == VISITED ||     // cross-edge
                    node.getVisited() == NOT_VISITED) { // forward-edge
                    try {
                        DerivedNode dn = (DerivedNode)node;
                        long tmpInterval = dn.getInterval();
                        _interval = MathUtil.lcm((int)_interval,
                                                 (int)tmpInterval);
                    } catch (ClassCastException e) {
                        // if we are a raw node, leave the interval as-is
                    }
                } else if (node.getVisited() == VISITING) {
                    // back-edge, this sub-graph is not valid
                    throw new CircularDependencyException("Cycle found: " + getId() +
                                                          " --> " + node.getId() );
                }
            }

            setVisited(VISITED);  // mark node as "already visited"
        }
        return _interval;
    }

    /**
     * Get all of the raw nodes depended upon by this derived node.
     */
    public Set getRawOutgoing() {
        HashSet rns = new HashSet();
        for (Iterator it=getOutgoing().iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof RawNode) {
                rns.add(o);
            }
        }

        return rns;
    }
}