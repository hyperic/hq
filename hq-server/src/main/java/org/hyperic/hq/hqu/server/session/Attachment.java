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

package org.hyperic.hq.hqu.server.session;

import org.hyperic.hibernate.PersistedObject;

public class Attachment
    extends PersistedObject 
{ 
    private View _view;
    private long         _attachTime;
    
    protected Attachment() {}
    
    Attachment(View view) {
        _view       = view;
        _attachTime = System.currentTimeMillis();
    }
    
    public View getView() {
        return _view;
    }
    
    protected void setView(View view) {
        _view = view;
    }
    
    public long getAttachTime() {
        return _attachTime;
    }
    
    protected void setAttachTime(long attachTime) {
        _attachTime = attachTime;
    }

    public String toString() {
        return _view.getPath() + " [" + _view.getDescription() + "] attached";
    }
    
    /**
     * TODO:  We probably need to subclass the attachments into their specific
     *        types (such as admin, etc.), via a Hibernate subclass.  This way
     *        each object can do a proper .equals()
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof Attachment)) {
            return false;
        }
        
        Attachment o = (Attachment)obj;
        return o.getView().equals(getView()) &&
            o.getAttachTime() == getAttachTime();
    }

    public int hashCode() {
        int result = 17;

        result = 37 * result + getView().hashCode();
        result = 37 * result + (int)getAttachTime();

        return result;
    }
}
