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

package org.hyperic.util.paramParser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ContainerAtom 
    extends FormatAtom
{
    private boolean   isORed;
    private ArrayList subAtoms;
    private String    blockName;
    
    ContainerAtom(){
        super();
        this.subAtoms  = new ArrayList();
        this.isORed    = false;
        this.blockName = null;
    }

    void setBlockName(String blockName){
        this.blockName = blockName;
    }

    String getBlockName(){
        return this.blockName;
    }

    void addSubAtom(FormatAtom subAtom){
        this.subAtoms.add(subAtom);
        this.checkSubAtoms();
    }

    List getSubAtoms(){
        if(this.isORed){
            ArrayList res = new ArrayList();

            for(Iterator i=this.subAtoms.iterator(); i.hasNext(); ){
                FormatAtom subAtom = (FormatAtom)i.next();

                if(!this.atomIsOR(subAtom)){
                    res.add(subAtom);
                }
            }
            return res;
        } else {
            return (List)(this.subAtoms.clone());
        }
    }

    boolean isORed(){
        return this.isORed;
    }

    private boolean atomIsOR(FormatAtom atom){
        return (atom instanceof LiteralAtom) &&
            ((LiteralAtom)atom).getLiteral().equals("|");
    }

    private void checkSubAtoms(){
        boolean lastWasOR, sawOne;

        for(Iterator i=this.getSubAtoms().iterator(); i.hasNext(); ){
            FormatAtom subAtom = (FormatAtom)i.next();

            if(this.atomIsOR(subAtom)){
                this.isORed = true;
            }
        }

        if(!this.isORed)
            return;

        lastWasOR = false;
        sawOne    = false;
        for(Iterator i=this.subAtoms.iterator(); i.hasNext(); ){
            FormatAtom subAtom = (FormatAtom)i.next();

            if(this.atomIsOR(subAtom)){
                if(sawOne && lastWasOR == true){
                    throw new FormatException("| must be seperated by " +
                                              "an intervening atom");
                }
                lastWasOR = true;
            } else {
                if(sawOne && lastWasOR == false){
                    throw new FormatException("| must be placed between " +
                                              "every atom in a '|' context");
                }
                lastWasOR = false;
            }
            sawOne = true;
        }
    }
}
