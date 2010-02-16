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

package org.hyperic.lather;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.util.TextIndenter;

/**
 * A utility class (which must reside in the same package as the
 * LatherValue class) which dumps the contents of a LatherValue
 * object to a stream.
 */
public class LatherDump {
    private PrintWriter pWriter;

    public LatherDump(OutputStream oStream){
        this.pWriter = new PrintWriter(oStream);
    }

    private String getClassOnly(Class fClass){
        String cName = fClass.getName();

        return cName.substring(cName.lastIndexOf(".") + 1);
    }

    private String getVal(Object o){
        if(o instanceof byte[]){
            return "[" + ((byte[])o).length + "]";
        } else {
            return o.toString();
        }
    }

    private void dumpEnt(TextIndenter tInd, Map map, String type){
        for(Iterator i=map.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();
            String val = this.getVal(ent.getValue());

            tInd.append(ent.getKey() + " = " + type + "(" + val + ")\n");
        }
    }

    private void dumpList(TextIndenter tInd, Map map, String type){
        for(Iterator i=map.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent = (Map.Entry)i.next();
            List l = (List)ent.getValue();

            tInd.append(ent.getKey() + " = [" + type + "]\n");
            tInd.pushIndent();

            for(Iterator j=l.iterator(); j.hasNext(); ){
                tInd.append(this.getVal(j.next()));
            }

            tInd.popIndent();
        }
    }

    private void dump(LatherValue val, TextIndenter tInd){
        tInd.append("[Dump of " + this.getClassOnly(val.getClass()) + "]\n");
        tInd.pushIndent();

        this.dumpEnt(tInd, val.getStringVals(), "String");
        this.dumpEnt(tInd, val.getIntVals(),    "Int");
        this.dumpEnt(tInd, val.getDoubleVals(), "Double");
        this.dumpEnt(tInd, val.getByteAVals(),  "ByteA");
        
        for(Iterator i=val.getObjectVals().entrySet().iterator(); 
            i.hasNext(); )
        {
            Map.Entry ent = (Map.Entry)i.next();

            tInd.append(ent.getKey() + " = Object()\n");
            this.dump((LatherValue)ent.getValue(), tInd);
        }

        this.dumpList(tInd, val.getStringLists(), "String");
        this.dumpList(tInd, val.getIntLists(),    "Int");
        this.dumpList(tInd, val.getDoubleLists(), "Double");
        this.dumpList(tInd, val.getByteALists(),  "ByteA");

        for(Iterator i=val.getObjectLists().entrySet().iterator(); 
            i.hasNext(); )
        {
            Map.Entry ent = (Map.Entry)i.next();
            List l = (List)ent.getValue();

            tInd.append(ent.getKey() + " = [Object]\n");
            for(Iterator j=l.iterator(); j.hasNext(); ){
                this.dump((LatherValue)j.next(), tInd);
            }
        }
        tInd.popIndent();
    }

    public void dump(LatherValue val){
        TextIndenter tInd = new TextIndenter();

        this.dump(val, tInd);
        this.pWriter.print(tInd.toString());
        this.pWriter.flush();
    }
}
