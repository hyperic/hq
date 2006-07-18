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

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.naming.NamingException;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.shared.MeasurementArgValue;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.measurement.shared.TemplateManagerUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Create a Graph structure from the MeasurementTemplate and
 * MeasurementArg data.
 *
 */
public final class GraphBuilder {
    private static final Log log = LogFactory.getLog(GraphBuilder.class);
    
    public static Graph buildGraph(Integer measurementTemplateId)
        throws InvalidGraphException, TemplateNotFoundException,
               SystemException
    {
        try {
            TemplateManagerLocal tmLocal =
                TemplateManagerUtil.getLocalHome().create();
            MeasurementTemplateValue mt =
                tmLocal.getTemplate(measurementTemplateId);
            return buildGraph(new Graph(), mt);
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    public static Graph buildGraph(Graph g, MeasurementTemplateValue mt)
        throws InvalidGraphException
    {
        try {
            // first, add this measurement if it's not there already
            Node current = g.getNode( mt.getId().intValue() );
            if (null == current) {
                current = new DerivedNode( mt.getId().intValue(),
                                           mt );
                g.addNode(current);
            }

            // now add our dependents, recursively
            MeasurementArgValue[] args = mt.getMeasurementArgs();
            for (int i = 0; i < args.length; i++) {
                MeasurementArgValue arg = args[i];
                MeasurementTemplateValue templ = arg.getMeasurementTemplateArg();
                if ( templ.getMeasurementArgs().length == 0) {
                    // raw measurement
                    RawNode n = (RawNode)g.getNode( templ.getId().intValue() );
                    if (null == n) {
                        n = new RawNode( templ.getId().intValue(),
                                         templ );
                        g.addNode(n);
                    }
                    g.addEdge( current.getId(), n.getId() );
                } else {
                    // derived measurement
                    DerivedNode n = (DerivedNode)g.getNode( templ.getId().intValue() );
                    boolean buildGraph = false;
                    if (null == n) {
                        n = new DerivedNode( templ.getId().intValue(),
                                             templ );
                        g.addNode(n);
                        buildGraph = true;
                    }
                    g.addEdge( current.getId(), n.getId() );
                    if (buildGraph) {
                        buildGraph(g, templ);
                    }
                }
            }

            return g;
        } catch (EJBException e) {
            //==========================================================
            // Well, this is not great.  We have to introspect any
            // EJBException that we catch to see if it was a "Reentrant
            // method call" problem.  This would imply a circular
            // dependency somewhere in our graph, so we should throw
            // such an exception.
            //
            // It's okay, because the TX will already be rolled back
            // by the time we catch the EJBException, so no harm is
            // done.
            //                       (jwescott)
            //==========================================================
            EJBException cause = e;
            while (null != cause) {
                String exMsg = cause.getMessage();
                if (exMsg.indexOf("Reentrant method call detected") == -1) {
                    try {
                        cause = (EJBException)cause.getCausedByException();
                    } catch (ClassCastException cce) {
                        cause = null;
                    }
                } else {
                    throw new CircularDependencyException
                        ("Circular dependency found while building graph.");
                }
            }
            throw e;
        }
    }

    //--------------------------------------------------------------------
    //-- static class, privatize ctor
    //--------------------------------------------------------------------
    private GraphBuilder() {
    }
}

// EOF
