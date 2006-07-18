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

package org.hyperic.hq.events.server.session;

import java.util.HashMap;
import java.util.StringTokenizer;

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.IDGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Entity EJB superclass, which provides generic utility
 * functions
 */
public abstract class SessionEJB {
    protected Log log = LogFactory.getLog(SessionEJB.class);

    /** the events database name */
    protected static final String DATASOURCE = HQConstants.DATASOURCE;
    /** the interval between sequence ID's */
    protected static final int SEQUENCE_INTERVAL = 1;

    // Initial context 
    private InitialContext ic = null;

    /** the static IDGenerator to generate ID's */
    private static HashMap idGenerators = new HashMap();

    protected InitialContext getInitialContext() throws NamingException {
        if (ic == null)
            ic = new InitialContext();

        return ic;
    }
    


    /** Get the next ID from the database sequence
     * @throws CreateException if the IDGenerator fails to generate a new ID
     * @return the next ID in the database sequence
     */
    protected Long getNextId(String seq) throws CreateException {
        IDGenerator idGen = (IDGenerator) idGenerators.get(seq);
        if (idGen == null) {
            idGen =
                new IDGenerator(
                    SessionEJB.class.getName(),
                    seq,
                    SEQUENCE_INTERVAL,
                    DATASOURCE);
            idGenerators.put(seq, idGen);
        }

        try {
            // Cast from long to Integer
            Long id = new Long(idGen.getNewID());
            if ( log.isDebugEnabled() ) {
                log.debug("New ID for " + seq + ": " + id);
            }
            return id;
        } catch (Exception e) {
            log.error("Exception while getting new sequence.", e);
            throw new CreateException(
                "Error getting new ID from IDGenerator: " + e);
        }
    }

}
