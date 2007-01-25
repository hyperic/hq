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
package org.hyperic.hq.zevents;

import java.util.Collections;

/**
 * This class is used as a link between {@link Runnable} and 
 * {@link ZeventListener}
 */
public class ZeventRunnableDecorator 
    implements Runnable
{
    private final Zevent         _event;
    private final ZeventListener _l;
 
    /**
     * @param event Event to pass to the listener when 
     *              {@link #run()} is invoked
     * @param l     Listener to invoke with the event when run is called
     */
    public ZeventRunnableDecorator(Zevent event, ZeventListener l) {
        _event = event;
        _l     = l;
    }
    
    public void run() {
        _l.processEvents(Collections.singletonList(_event));
    }
}
