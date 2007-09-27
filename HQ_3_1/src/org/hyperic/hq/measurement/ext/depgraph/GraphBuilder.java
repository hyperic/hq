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

import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.server.session.TemplateManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementArg;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;

import java.util.Collection;
import java.util.Iterator;

/**
 * Create a Graph structure from the MeasurementTemplate and
 * MeasurementArg data.
 *
 */
public final class GraphBuilder {

    private GraphBuilder() {}
    
    public static Graph buildGraph(Integer id)
        throws InvalidGraphException, TemplateNotFoundException
    {
        TemplateManagerLocal tmLocal = TemplateManagerEJBImpl.getOne();
        MeasurementTemplate mt = tmLocal.getTemplate(id);

        return buildGraph(new Graph(), mt);
    }

    public static Graph buildGraph(MeasurementTemplate mt)
        throws InvalidGraphException
    {
        return buildGraph(new Graph(), mt);
    }

    private static Graph buildGraph(Graph g, MeasurementTemplate mt)
        throws InvalidGraphException
    {
        // first, add this measurement if it's not there already
        Node current = g.getNode(mt.getId().intValue());
        if (null == current) {
            current = new DerivedNode(mt.getId().intValue(), mt);
            g.addNode(current);
        }

        // now add our dependents, recursively
        Collection args = mt.getMeasurementArgs();
        for (Iterator i = args.iterator(); i.hasNext(); ) {
            MeasurementArg arg = (MeasurementArg)i.next();
            MeasurementTemplate templ = arg.getTemplateArg();
            if (templ.getMeasurementArgs().size() == 0) {
                // raw measurement
                RawNode n = (RawNode) g.getNode(templ.getId().intValue());
                if (null == n) {
                    n = new RawNode(templ.getId().intValue(), templ);
                    g.addNode(n);
                }
                g.addEdge(current.getId(), n.getId());
            } else {
                // derived measurement
                DerivedNode n = (DerivedNode) g.getNode(templ.getId().intValue());
                boolean buildGraph = false;
                if (null == n) {
                    n = new DerivedNode(templ.getId().intValue(), templ);
                    g.addNode(n);
                    buildGraph = true;
                }
                g.addEdge(current.getId(), n.getId());
                if (buildGraph) {
                    buildGraph(g, templ);
                }
            }
        }

        return g;
    }
}

