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

package org.hyperic.hq.bizapp.client.shell;

import org.hyperic.util.paramParser.BasicRetriever;
import org.hyperic.util.paramParser.FormatException;
import org.hyperic.util.paramParser.FormatParser;
import org.hyperic.util.paramParser.ParserRetriever;

class ClientShellParserRetriever
    extends BasicRetriever
{
    private ClientShellEntityFetcher entityFetcher;

    ClientShellParserRetriever(ClientShellEntityFetcher entityFetcher){
        super();
        this.entityFetcher = entityFetcher;
    }

    public FormatParser getParser(String className){
        if(className.equals("PlatformTag"))
            return this.getParserInternal(PlatformTagParser.class.getName());
        else if(className.equals("ServerTag"))
            return this.getParserInternal(ServerTagParser.class.getName());
        else if(className.equals("ServiceTag"))
            return this.getParserInternal(ServiceTagParser.class.getName());
        else if(className.equals("ApplicationTag"))
           return this.getParserInternal(ApplicationTagParser.class.getName());
        else if(className.equals("GroupTag"))
            return this.getParserInternal(GroupTagParser.class.getName());
        else if(className.equals("PlatformTypeTag"))
            return this.getParserInternal(PlatformTypeTagParser.class.getName());
        else if(className.equals("ServerTypeTag"))
            return this.getParserInternal(ServerTypeTagParser.class.getName());
        else if(className.equals("ServiceTypeTag"))
            return this.getParserInternal(ServiceTypeTagParser.class.getName());
        else
            return this.getParserInternal(className);
    }

    private FormatParser getParserInternal(String className){
        FormatParser res;

        try {
            res = super.getParser(className);
        } catch(FormatException exc){
            try {
                res = super.getParser("org.hyperic.hq.bizapp." +
                                      "client.shell." + className);
            } catch(FormatException iExc){
                throw exc;
            }
        }

        if(res instanceof ClientShellFormatParser){
            ((ClientShellFormatParser)res).init(this.entityFetcher);
        }
        return res;
    }    
}
