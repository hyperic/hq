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

package org.hyperic.hq.escalation.server.session;


/**
 * This is a utility class meant to provide some of the basic pieces needed
 * for things to implement {@link Escalatable} 
 */
public abstract class EscalatableBase 
    implements Escalatable
{
    private PerformsEscalations _def;
    private Integer             _id;
    private String              _shortReason;
    private String              _longReason;
    private boolean             _acknowledgeable;
    
    protected EscalatableBase(PerformsEscalations def, Integer id,
                              String shortReason, String longReason,
                              boolean ackable) 
    {
        _def             = def;
        _id              = id;
        _shortReason     = shortReason;
        _longReason      = longReason;
        _acknowledgeable = ackable;
    }
    
    public PerformsEscalations getDefinition() {
        return _def;
    }

    public Integer getId() {
        return _id;
    }

    public String getLongReason() {
        return _longReason;
    }

    public String getShortReason() {
        return _shortReason;
    }

    public boolean isAcknowledgeable() {
        return _acknowledgeable;
    }
}
