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

package org.hyperic.hq.events.ext;

import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;

public class RegisteredTriggerEvent extends AbstractEvent implements java.io.Serializable {
    
    public static final int ADD    = 1;
    public static final int DELETE = 2;
    public static final int UPDATE = 3;
    public static final int FIRED  = 4;
    
    /** Holds value of property value. */
    private RegisteredTriggerValue value;
    
    /** Holds value of property action. */
    private int action;
    
    /** Creates a new instance of TriggerEvent */
    public RegisteredTriggerEvent(int action, RegisteredTriggerValue value) {
        super.setInstanceId(value.getId());
        super.setTimestamp(System.currentTimeMillis());
        this.action = action;
        this.value = value;
    }
    
    /** Getter for property value.
     * @return Value of property value.
     *
     */
    public RegisteredTriggerValue getValue() {
        return this.value;
    }
    
    /** Setter for property value.
     * @param value New value of property value.
     *
     */
    public void setValue(RegisteredTriggerValue value) {
        this.value = value;
    }
    
    /** Getter for property action.
     * @return Value of property action.
     *
     */
    public int getAction() {
        return this.action;
    }
    
    /** Setter for property action.
     * @param action New value of property action.
     *
     */
    public void setAction(int action) {
        this.action = action;
    }
}
