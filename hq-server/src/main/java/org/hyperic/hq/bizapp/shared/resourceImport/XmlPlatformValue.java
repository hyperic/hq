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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class XmlPlatformValue 
    extends XmlResourceValue
{
    private static final String ATTR_CERTDN   = "certdn";
    private static final String ATTR_COMMENT  = "comment";
    private static final String ATTR_CPUCOUNT = "cpucount";
    private static final String ATTR_CPUSPEED = "cpuspeed";
    private static final String ATTR_FQDN     = "fqdn";
    private static final String ATTR_RAM      = "ram";

    private static final String[] ATTRS_REQUIRED = {
        ATTR_FQDN,
        XmlResourceValue.ATTR_NAME,
        XmlResourceValue.ATTR_TYPE,
    };

    private static final String[] ATTRS_OPTIONAL = {
        ATTR_CERTDN,
        ATTR_COMMENT,
        ATTR_CPUCOUNT,
        ATTR_CPUSPEED,
        ATTR_RAM,
        XmlResourceValue.ATTR_DESCRIPTION,
        XmlResourceValue.ATTR_LOCATION,
    };

    private ArrayList            ips;
    private ArrayList            servers;
    private XmlAgentConnValue agentConn;

    XmlPlatformValue(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
        this.ips     = new ArrayList();
        this.servers = new ArrayList();
    }
    
    public static String[] getRequiredAttributes(){
        return ATTRS_REQUIRED;
    }

    public static String[] getOptionalAttributes(){
        return ATTRS_OPTIONAL;
    }

    void setAgentConn(XmlAgentConnValue agentConn){
        this.agentConn = agentConn;
    }

    void addIP(XmlIpValue val){
        this.ips.add(val);
    }
    
    void addServer(XmlServerValue val){
        this.servers.add(val);
    }

    public XmlAgentConnValue getAgentConn(){
        return this.agentConn;
    }

    public String getFqdn(){
        return this.getValue(ATTR_FQDN);
    }

    public String getCertdn(){
        return this.getValue(ATTR_CERTDN);
    }

    public String getComment(){
        return this.getValue(ATTR_COMMENT);
    }

    public Integer getCpuCount(){
        String val = this.getValue(ATTR_CPUCOUNT);

        return val == null ? new Integer(1) : Integer.valueOf(val);
    }

    public Integer getCpuSpeed(){
        String val = this.getValue(ATTR_CPUSPEED);

        return val == null ? null : Integer.valueOf(val);
    }

    public Integer getRam(){
        String val = this.getValue(ATTR_RAM);
        
        return val == null ? null : Integer.valueOf(val);
    }

    public List getIpValues(){
        return this.ips;
    }

    public List getServers(){
        return this.servers;
    }

    void setValue(String key, String value)
        throws XmlInvalidAttrException
    {
        if(key.equals(ATTR_CPUCOUNT) ||
           key.equals(ATTR_CPUSPEED) ||
           key.equals(ATTR_RAM))
        {
            try {
                Integer.parseInt(value);
            } catch(NumberFormatException exc){
                throw new XmlInvalidAttrException("'" + key + "' attribute"+
                                                     " must be an integer");
            }
        }
        super.setValue(key, value);
    }

    void validate()
        throws XmlValidationException
    {
        HashSet h;

        super.validate();

        h = new HashSet();
        for(Iterator i=this.servers.iterator(); i.hasNext(); ){
            XmlServerValue sVal = (XmlServerValue)i.next();
            String name = sVal.getName();
            
            h.add(name);
            sVal.validate();
        }

        if (this.ips.size() == 0) {
            throw new XmlValidationException("No ip address defined");
        }

        h = new HashSet();
        for(Iterator i=this.ips.iterator(); i.hasNext(); ){
            XmlIpValue iVal = (XmlIpValue)i.next();
            String ip = iVal.getAddress();

            if(h.contains(ip)){
                throw new XmlValidationException("Platform '" + 
                                                    this.getName() + "' has " +
                                                    "ip address '" + ip + 
                                                    "' defined > 1 times");
            }
            h.add(ip);
            iVal.validate();
        }
    }

    public String toString(){
        return super.toString() + " " + this.ips + " " + this.agentConn + " " +
            this.servers;
    }
}
