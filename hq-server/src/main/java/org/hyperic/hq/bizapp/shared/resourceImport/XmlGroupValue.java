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

public class XmlGroupValue
    extends XmlValue
{
    public static final String T_COMPAT    = "compat";
    public static final String T_ADHOC     = "adhoc";
    public static final String N_PLATFORM  = "platform";
    public static final String N_SERVER    = "server";
    public static final String N_SERVICE   = "service";
    public static final String N_GROUP     = "group";
    public static final String N_APP       = "application";
    public static final String N_MIXED     = "mixed";

    private static final String ATTR_MEMBERTYPE     = "membertype";
    private static final String ATTR_MEMBERTYPENAME = "membertypename";

    private static final String[] ATTRS_REQUIRED = {
        XmlResourceValue.ATTR_NAME,
        XmlResourceValue.ATTR_TYPE,
        ATTR_MEMBERTYPE,
    };

    private static final String[] ATTRS_OPTIONAL = {
        XmlResourceValue.ATTR_DESCRIPTION,
        XmlResourceValue.ATTR_LOCATION,
        ATTR_MEMBERTYPENAME,
    };

    private List                 members;

    XmlGroupValue(){
        super(ATTRS_REQUIRED, ATTRS_OPTIONAL);
        this.members = new ArrayList();
    }
    
    public static String[] getRequiredAttributes(){
        return ATTRS_REQUIRED;
    }

    public static String[] getOptionalAttributes(){
        return ATTRS_OPTIONAL;
    }

    public String getName(){
        return this.getValue(XmlResourceValue.ATTR_NAME);
    }

    public String getCapName(){
        return this.getValue(XmlResourceValue.ATTR_NAME);
    }

    public String getType(){
        return this.getValue(XmlResourceValue.ATTR_TYPE);
    }

    public String getMemberType(){
        return this.getValue(ATTR_MEMBERTYPE);
    }

    public String getMemberTypeName(){
        return this.getValue(ATTR_MEMBERTYPENAME);
    }

    public String getDescription(){
        return this.getValue(XmlResourceValue.ATTR_DESCRIPTION);
    }
    
    public String getLocation(){
        return this.getValue(XmlResourceValue.ATTR_LOCATION);
    }

    public void addMember(XmlGroupMemberValue member){
        this.members.add(member);
    }

    public List getMembers(){
        return this.members;
    }

    void endAttributes()
        throws XmlInvalidAttrException
    {
        final String ERR_BEGIN = "<" + GroupTag.TAG_NAME + "> requires a ";
        String type, memberType;

        type = this.getType();
        if(!(type.equals(T_ADHOC) || type.equals(T_COMPAT))){
            throw new XmlInvalidAttrException(ERR_BEGIN + 
                            XmlResourceValue.ATTR_TYPE +
                            " attribute with a value of 'adhoc' or 'compat': "+
                            "value given was '" + type + "'");
        }
    
        memberType = this.getMemberType();
        if(type.equals(T_ADHOC)){
            if(!(memberType.equals(N_GROUP) ||
                 memberType.equals(N_APP) ||
                 memberType.equals(N_MIXED)))
            {
                throw new XmlInvalidAttrException("adhoc " + ERR_BEGIN + 
                             ATTR_MEMBERTYPE + " attribute with a value of " +
                             "'group', 'application', or 'mixed': " + 
                             "value given was '" + memberType + "'");
            }

            if(this.getMemberTypeName() != null){
                throw new XmlInvalidAttrException(ERR_BEGIN + 
                                    ATTR_MEMBERTYPENAME + " attribute, only " +
                                    "if " + ATTR_MEMBERTYPE + " = 'compat'");
            }
        } else {
            if(!(memberType.equals(N_PLATFORM) ||
                 memberType.equals(N_SERVER) ||
                 memberType.equals(N_SERVICE)))
            {
                throw new XmlInvalidAttrException("compat " + ERR_BEGIN +
                              ATTR_MEMBERTYPE + " attribute with a value of " +
                              "'platform', 'server', or 'service': " +
                              "value given was '" + memberType + "'");
            }

            if(this.getMemberTypeName() == null){
                throw new XmlInvalidAttrException(ERR_BEGIN + 
                                    ATTR_MEMBERTYPENAME + " attribute if " +
                                    ATTR_MEMBERTYPE + " = 'compat'");
            }
        }
    }

    void setValue(String key, String value)
        throws XmlInvalidAttrException
    {
        super.setValue(key, value);
    }

    void validate()
        throws XmlValidationException
    {
        HashSet memSet;

        super.validate();

        // Make sure all the members work for the given memberType/etc.
        memSet = new HashSet();
        for(Iterator i=this.members.iterator(); i.hasNext(); ){
            XmlGroupMemberValue member = (XmlGroupMemberValue)i.next();
            String memberType = member.getType();

            if(memSet.contains(member.getName())){
                throw new XmlValidationException("Member '" + 
                                                    member.getName() + "' is "+
                                                    "contained > 1 time in " +
                                                    "group '" + 
                                                    this.getName() + "'");
            }
            memSet.add(member.getName());

            if(memberType != null){
                if(this.getType().equals(T_ADHOC) &&
                   this.getMemberType().equals(N_MIXED))
                {
                    if(!(memberType.equals(N_PLATFORM) ||
                         memberType.equals(N_SERVER) ||
                         memberType.equals(N_SERVICE)))
                    {
                        throw new XmlValidationException("Member '" +
                                 member.getName() + "' of type '" +
                                 memberType + "' is not valid in group '" +
                                 this.getName() + "' because it is a mixed " +
                                 "adhoc group");
                    }
                } else {
                    if(!memberType.equals(this.getMemberType())){
                        throw new XmlValidationException("Member '" +
                                member.getName() + "' of type '" + memberType +
                                "' is not valid in group '" + this.getName() +
                                "' because it requires all members to be of "+
                                "type '" + this.getMemberType() + "'");
                    }
                }
            } else {
                if(this.getType().equals(T_ADHOC) &&
                   this.getMemberType().equals(N_MIXED))
                {
                    throw new XmlValidationException("Member '" +
                             member.getName() + "' must have a " + 
                             XmlResourceValue.ATTR_TYPE + 
                             " attribute, because it is in group '" + 
                             this.getName() + "' which is mixed");
                }
            }
        }
    }

    public String toString(){
        return super.toString() + 
            " MEMBERS=" + this.members;
    }
}
