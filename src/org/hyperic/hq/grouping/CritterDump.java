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

package org.hyperic.hq.grouping;

import java.math.BigDecimal;

import org.hyperic.hq.authz.server.session.Resource;

/**
 * A CritterDump represents state which a Critter can store into (or be
 * reconstructed from.
 * 
 * Each of these fields represent 'generic' data that only a specific
 * {@link CritterType} knows about.
 * 
 * This interface is also used to abstract out persistence, so that critters
 * are able to persist to anything implementing this method.
 * 
 * @see CritterType#compose(CritterDump)
 * @see CritterType#decompose(Critter, CritterDump)
 */
public interface CritterDump {
    String getStringProp();
    void setStringProp(String s);
    
    Long getDateProp();
    void setDateProp(Long date);
    
    Resource getResourceProp();
    void setResourceProp(Resource r);
    
    BigDecimal getNumericProp();
    void setNumericProp(BigDecimal n);
    
    Integer getEnumProp();
    void setEnumProp(Integer e);
}
