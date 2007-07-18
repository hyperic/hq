/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2007], Hyperic, Inc. 
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

package org.hyperic.hq.control;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.LoggableInterface;
import org.hyperic.hq.events.ResourceEventInterface;

public class ControlEvent extends AbstractEvent
    implements java.io.Serializable, ResourceEventInterface, LoggableInterface {
    
    /** Holds value of property action. */
    private String action;

    /** Holds value of property subject. */
    private String subject;

    private AppdefEntityID resource;

    private boolean scheduled;
    private long    dateScheduled;
    private String  status;

    /** Creates a new instance of ControlEvent */
    public ControlEvent(String subject, int resourcetype, Integer resourceId,
                        String action, boolean scheduled, long dateScheduled,
                        String status) {
        super.setInstanceId(resourceId);
        super.setTimestamp(System.currentTimeMillis());
        this.subject  = subject;
        this.resource = 
            new AppdefEntityID(resourcetype, resourceId.intValue());
        this.action        = action;
        this.scheduled     = scheduled;
        this.dateScheduled = dateScheduled;
        this.status        = status;
    }
    
    /** Getter for property action.
     * @return Value of property action.
     *
     */
    public String getAction() {
        return this.action;
    }
    
    /** Setter for property action.
     * @param action New value of property action.
     *
     */
    public void setAction(String action) {
        this.action = action;
    }
    
    /** Getter for property subject.
     * @return Value of property subject.
     *
     */
    public String getSubject() {
        return this.subject;
    }
    
    /** Setter for property subject.
     * @param subject New value of property subject.
     *
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    /** Getter for property resource.
     * @return Value of property resource.
     *
     */
    public AppdefEntityID getResource(){
        return this.resource;
    }

    /** Getter for property resource.
     * @return Value of property resource.
     *
     */
    private void setResource(AppdefEntityID resource) {
        this.resource = resource;
    }

    /**
     * Getter for property scheduled
     */
    public boolean getScheduled() {
        return this.scheduled;
    }

    /**
     * Setter for property scheduled
     */
    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    /**
     * Getter for property dateScheduled
     */
    public long getDateScheduled() {
        return this.dateScheduled;
    }

    /**
     * Setter for property dateScheduled
     */
    public void setDateScheduled(long dateScheduled) {
        this.dateScheduled = dateScheduled;
    }

    /**
     * Getter for property status
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * Setter for property status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /** Returns a string representation of the object. In general, the
     * <code>toString</code> method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The <code>toString</code> method for class <code>Object</code>
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `<code>@</code>', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return  a string representation of the object.
     *
     */
    public String toString() {
        return getAction();
    }

    public String getLevelString() {
        return getStatus();
    }    
}
