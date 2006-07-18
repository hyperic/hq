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

/**
 * The BasicRetriever is a ParserRetriever which performs 
 * simple newInstance() style instantiation of parsing classes.
 */
public class BasicRetriever
    implements ParserRetriever
{
    public FormatParser getParser(String className){
        if(className.equals("Integer")){
            return this.getParserInternal(IntegerParser.class.getName());
        } else if(className.equals("String")){
            return this.getParserInternal(StringParser.class.getName());
        } else if(className.equals("FutureDate")){
            return this.getParserInternal(FutureDateParser.class.getName());
        } else if(className.equals("PastDate")){
            return this.getParserInternal(PastDateParser.class.getName());
        } else if(className.equals("Interval")){
            return this.getParserInternal(IntervalParser.class.getName());
        }

        try {
            return this.getParserInternal(className);
        } catch(FormatException exc){
            try {
                return this.getParserInternal("org.hyperic.util.paramParser."+
                                              className);
            } catch(FormatException ignExc){
                throw exc;
            }
        }
    }

    private FormatParser getParserInternal(String className){
        try {
            Class lClass;
            Object lObj;

            lClass = Class.forName(className);
            lObj   = lClass.newInstance();

            if(!(lObj instanceof FormatParser)){
                throw new FormatException("Formatting class '" + className + 
                                          "' does not implement FormatParser");
            }

            return (FormatParser)lObj;
        } catch(ClassNotFoundException exc){
            throw new FormatException("Unable to find formatting class '" +
                                      className + "'");
        } catch(InstantiationException exc){
            throw new FormatException("Unable to instantiate formatting " +
                                      "class '" + className + "': " + 
                                      exc.getMessage());
        } catch(IllegalAccessException exc){
            throw new FormatException("Illegal access exception generated " +
                                      "while instantiating formatting class '"+
                                      className + "': " + exc.getMessage());
        }
    }
}
