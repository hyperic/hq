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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Determines the dependent measurements for a given derived
 * measurement.
 *
 */
public class Graph implements java.io.Serializable {
    private static final String EOL = System.getProperty("line.separator");
    public static final Integer INVALID = new Integer(-1);
    private Map nodes = new TreeMap();

    public Graph() {
    }

    /**
     * Mark all nodes in this graph as <code>Node.NOT_VISITED</code>.
     */
    public void reset() {
        for (Iterator it=nodes.values().iterator(); it.hasNext();) {
            ( (Node) it.next() ).setVisited(Node.NOT_VISITED);
        }
    }

    /**
     * Get an ordered list of all Nodes that must be computed before
     * the Node with the given id can be computed.
     *
     * @param id the id of the node whose ordering is wanted
     * @return a <code>{@link java.util.LinkedList}</code> of <code>
     * {@link org.hyperic.hq.measurement.ext.depgraph.Node}</code>
     */
    public LinkedList getOrderedOutgoing(int id) {
        reset();
        Node node = getNode(id);
        LinkedList stack = new LinkedList();
        node.pushOutgoing(stack);
        return stack;
    }

    /**
     * Add the given node to the graph, removing any node already
     * contained in the graph with the same id.
     *
     * @param n the node to add
     */
    public void addNode(Node n) {
        nodes.put(new Integer( n.getId() ), n);
    }

    /**
     * Get the node for the given id.  If the node does not exist,
     * null will be returned.
     *
     * @param id the id of the node to get
     */
    public Node getNode(int id) {
        return (Node)nodes.get( new Integer(id) );
    }

    /**
     * Add an edge from the first to the second node in the graph.
     *
     * @throws InvalidGraphException if n1 is a RawNode
     */
    public void addEdge(int first, int second) throws InvalidGraphException {
        Node n1 = getNode(first);
        Node n2 = getNode(second);
        n1.addOutgoing(n2);
        n2.addIncoming(n1);
    }

    /**
     * Get all of the nodes in this graph.
     *
     * @return <code>{@link java.util.Collection}</code> of
     * <code>{@link
     * org.hyperic.hq.measurement.ext.depgraph.Node}</code>.
     */
    public Collection getNodes() {
        return nodes.values();
    }

    /**
     * Get all of the derived nodes in this graph.
     *
     * @return <code>{@link java.util.Collection}</code> of
     * <code>{@link
     * org.hyperic.hq.measurement.ext.depgraph.DerivedNode}</code>.
     */
    public Set getDerivedNodes() {
        HashSet dns = new HashSet();
        for (Iterator it=nodes.values().iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof DerivedNode) {
                dns.add(o);
            }
        }

        return dns;
    }

    /**
     * Return a string-representation of this graph.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(EOL);
        for (Iterator it=nodes.values().iterator(); it.hasNext();) {
            Node node = (Node)it.next();
            if (node instanceof RawNode) {
                RawNode rn = (RawNode)node;
                sb.append("\t[ RAW ");
                sb.append( rn.getId() );
                sb.append(" ] ");
                appendIncoming(sb, node);
                sb.append(EOL);
            } else { // (node instanceof DerivedNode)
                DerivedNode dn = (DerivedNode)node;
                sb.append("\t[ DERIVED ");
                sb.append( dn.getId() );
                sb.append(" ] ");
                appendIncoming(sb, node);
                sb.append("; ");
                appendOutgoing(sb, node);
                sb.append(EOL);
            }
        }

        return sb.toString();
    }

    private void appendIncoming(StringBuffer sb, Node node) {
        sb.append("incoming: ");
        for (Iterator jt=node.getIncoming().iterator(); jt.hasNext();) {
            DerivedNode dn = (DerivedNode)jt.next();
            sb.append( dn.getId() );
            if ( jt.hasNext() ) {
                sb.append(", ");
            }
        }
    }

    private void appendOutgoing(StringBuffer sb, Node node) {
        sb.append("outgoing: ");
        for (Iterator jt=node.getOutgoing().iterator(); jt.hasNext();) {
            Node out = (Node)jt.next();
            sb.append( out.getId() );
            if ( jt.hasNext() ) {
                sb.append(", ");
            }
        }
    }
}

