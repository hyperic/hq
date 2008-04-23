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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds a list of critters.
 */
public class CritterList {
    private List    _critters;
    private boolean _isAny;
    
    /**
     * @param critters list of {@link Critter}s
     * @param isAny    if true, any critter can be matched for the list to
     *                 succeed (OR condition), else AND
     */
    public CritterList(List critters, boolean isAny) {
        _critters = new ArrayList(critters);
        _isAny    = isAny;
    }
    
    public List getCritters() {
        return Collections.unmodifiableList(_critters);
    }
    
    /**
     * 
     * @return true iff the list of criteria represents a logical OR of its 
     * constituent criteria
     */
    public boolean isAny() {
        return _isAny;
    }
    
    /**
     * 
     * @return true iff the list of criteria represents a logical AND of its 
     * constituent criteria
     */
    public boolean isAll() {
        return !_isAny;
    }
    

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CritterList)) return false;

        final CritterList critterList = (CritterList) other;
        if (critterList.isAny() != isAny()) return false;
        
        List listObj = critterList.getCritters();
        List origListObj = getCritters();
        
        // the critter list should never be null
        assert listObj != null;
        assert origListObj != null;
        
        // verify that the list of critters contains
        // logically equivalent critters in the same order
        if (listObj.size() != origListObj.size())
            return false;
        else {
            for (int i =0; i < listObj.size(); i++) {
                if (!listObj.get(i).equals(origListObj.get(i)))
                        return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int result;
        result = getCritters().hashCode();
        result = 29 * result + (isAny() ? 0 : 1);
        return result;
    }
}
