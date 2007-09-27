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

package org.hyperic.hq.bizapp.shared.resourceImport;

import org.hyperic.util.xmlparser.XmlParser;
import org.hyperic.util.xmlparser.XmlAttr;
import org.hyperic.util.xmlparser.XmlParseException;

import java.io.File;
import java.io.PrintStream;

public class Parser {
    public static BatchImportData parse(File in) 
        throws BatchImportException
    {
        BatchImportData res;
        RootTag tag;
        
        res = new BatchImportData();
        tag = new RootTag(res);
        try {
            XmlParser.parse(in, tag);
        } catch(XmlParseException exc){
            throw new BatchImportException(exc.getMessage());
        }
        return res;
    }

    /**
     * Convert lists of required and optional attributes to a XmlAttr
     * array.
     */
    static XmlAttr[] convertGhettoToAttrs(String[] required, 
                                               String[] optional)
    {
        XmlAttr[] res;
        int idx;

        res = new XmlAttr[required.length + optional.length];
        idx = 0;
        for(int i=0; i<required.length; i++){
            res[idx++] = new XmlAttr(required[i], XmlAttr.REQUIRED);
        }

        for(int i=0; i<optional.length; i++){
            res[idx++] = new XmlAttr(optional[i], XmlAttr.OPTIONAL);
        }
        return res;
    }

    public static void dumpFormat(PrintStream out){
        XmlParser.dump(new RootTag(null), out);
    }

    public static void dumpWiki(PrintStream out){
        XmlParser.dumpWiki(new RootTag(null), out);
    }

    public static void main(String[] args) throws Exception {
        BatchImportData data;

        if(args.length == 2 && args[0].equals("parse")){
            data = Parser.parse(new File(args[0]));
            Validator.validate(data);
            System.out.println(data);
        } else if(args.length == 1 && args[0].equals("dump")){
            Parser.dumpFormat(System.out);
        } else if(args.length == 1 && args[0].equals("wiki")){
            Parser.dumpWiki(System.out);
        }           
    }
}
