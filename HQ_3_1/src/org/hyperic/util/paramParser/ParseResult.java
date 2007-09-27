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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hyperic.util.TextIndenter;


public class ParseResult {
    private HashMap     props;
    private ParseResult parent;
    private ArrayList   children;
    private String      blockName;

    public ParseResult(String blockName){
        this.props     = new HashMap();
        this.parent    = null;
        this.children  = new ArrayList();
        this.blockName = blockName;
    }

    public String getBlockName(){
        return this.blockName;
    }

    public ParseResult getRoot(){
        ParseResult res;

        res = this;
        while(res.parent != null){
            res = res.parent;
        }
        
        return res;
    }

    public ParseResult getParent(){
        return this.parent;
    }

    public void addChild(ParseResult child){
        this.children.add(child);
        child.parent = this;
    }

    public void removeChild(ParseResult child){
        this.children.remove(child);
        child.parent = null;
    }

    public List getChildren(){
        return this.children;
    }

    public Object getValue(String key){
        return this.props.get(key);
    }

    public void setValue(String key, Object value){
        this.props.put(key, value);
    }

    public Set getKeys(){
        return this.props.keySet();
    }

    void toString(TextIndenter tInd){
        tInd.append("Block: " + this.getBlockName() + "\n");
        tInd.pushIndent();

        for(Iterator i=this.getKeys().iterator(); i.hasNext(); ){
            String key = (String)i.next();

            tInd.append(key + "='" + this.getValue(key) + "'\n");
        }

        tInd.pushIndent();
        for(Iterator i=this.getChildren().iterator(); i.hasNext(); ){
            ParseResult child = (ParseResult)i.next();

            child.toString(tInd);
        }
        tInd.popIndent();
        tInd.popIndent();
    }

    public String toString(){
        TextIndenter res;

        res = new TextIndenter(2);
        this.toString(res);
        return res.toString();
    }
}
