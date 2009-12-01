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

/*
 * OptionItem.java
 *
 * Created on February 26, 2003, 10:50 AM
 */

package org.hyperic.hq.ui.beans;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.struts.util.LabelValueBean;

import org.hyperic.util.StringUtil;

/**
 * This bean is for use with html:options. 
 *
 */
public class OptionItem extends LabelValueBean implements java.io.Serializable {
    
    public OptionItem() {
        this(null, null);
    }
    
    public OptionItem(String lab, String val) {
        super(lab, val);
    }
    
    /**
     * Create a list of OptionItems from a list
     * of strings.
     *
     * @param lofs a java.util.List of Strings.
     * @return A list of OptionItem objects with
     *         value and label set.
     */
    public static List createOptionsList(List lofs) {
        ArrayList newList = new ArrayList(lofs.size());
        Iterator i = lofs.iterator();
        OptionItem item;
        while (i.hasNext()) {
            String value = (String)i.next();
            String label = StringUtil.capitalize(value);
            item = new OptionItem(label, value);
            newList.add(item);
        }
        return newList;
    }
        
}
