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

package org.hyperic.hq.common;

import java.util.ResourceBundle;

import org.hyperic.util.HypericEnum;

public class YesOrNo 
    extends HypericEnum 
{
    private static final String BUNDLE = "org.hyperic.hq.common.Resources";
    
    public static final YesOrNo YES = new YesOrNo(0, "yes", "yes");
    
    public static final YesOrNo NO  = new YesOrNo(1, "no", "no");
    
    private YesOrNo(int code, String desc, String localeProp) {
        super(code, desc, localeProp, ResourceBundle.getBundle(BUNDLE));
    }
    
    public static YesOrNo valueFor(boolean isYes) {
        return isYes ? YES : NO;
    }
    
    /**
     * Return the <code>YesOrNo</code> equivalent of the string value.
     * 
     * @param yesOrNo The string value.
     * @return <code>YesOrNo.YES</code> if the case insensitive trimmed string 
     *         value is <code>y</code> or <code>yes</code>; 
     *         otherwise return <code>YesOrNo.NO</code>.
     */
    public static YesOrNo valueFor(String yesOrNo) {
        if (yesOrNo == null) {
            return NO;
        }
        
        String normalizedValue = yesOrNo.trim().toLowerCase();
        
        if (normalizedValue.equals("y")) {
            return YES;
        } else if (normalizedValue.equals("n")) {
            return NO;
        }
        
        YesOrNo value = 
            (YesOrNo)findByDescription(YesOrNo.class, normalizedValue);
        
        if (value == null) {
            return NO;
        } else {
            return value;
        }
    }
    
    /**
     * @return The Boolean equivalent.
     */
    public Boolean toBoolean() {
        if (YES.equals(this)) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }
    
}
