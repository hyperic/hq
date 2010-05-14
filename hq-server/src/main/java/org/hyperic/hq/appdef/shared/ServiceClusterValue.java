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
 * xdoclet generated code.
 * legacy DTO pattern (targeted to be replaced with hibernate pojo).
 */
package org.hyperic.hq.appdef.shared;

/**
 * Value object for ServiceCluster.
 *
 */
public class ServiceClusterValue extends AppdefResourceValue
   implements java.io.Serializable
{
    private String name;
    private boolean nameHasBeenSet = false;
    private String sortName;
    private boolean sortNameHasBeenSet = false;
    private String description;
    private boolean descriptionHasBeenSet = false;
    private Integer groupId;
    private boolean groupIdHasBeenSet = false;
    private String owner;
    private boolean ownerHasBeenSet = false;
    private String modifiedBy;
    private boolean modifiedByHasBeenSet = false;
    private String location;
    private boolean locationHasBeenSet = false;
    private Integer id;
    private boolean idHasBeenSet = false;
    private Long mTime;
    private boolean mTimeHasBeenSet = false;
    private Long cTime;
    private boolean cTimeHasBeenSet = false;
    private ServiceTypeValue ServiceType;
    private boolean ServiceTypeHasBeenSet = false;

    public ServiceClusterValue()
    {
    }

    public ServiceClusterValue( String name,String sortName,String description,Integer groupId,String owner,String modifiedBy,String location,Integer id,Long mTime,Long cTime )
    {
        this.name = name;
        nameHasBeenSet = true;
        this.sortName = sortName;
        sortNameHasBeenSet = true;
        this.description = description;
        descriptionHasBeenSet = true;
        this.groupId = groupId;
        groupIdHasBeenSet = true;
        this.owner = owner;
        ownerHasBeenSet = true;
        this.modifiedBy = modifiedBy;
        modifiedByHasBeenSet = true;
        this.location = location;
        locationHasBeenSet = true;
        this.id = id;
        idHasBeenSet = true;
        this.mTime = mTime;
        mTimeHasBeenSet = true;
        this.cTime = cTime;
        cTimeHasBeenSet = true;
    }

    //TODO Cloneable is better than this !
    public ServiceClusterValue( ServiceClusterValue otherValue )
    {
        this.name = otherValue.name;
        nameHasBeenSet = true;
        this.sortName = otherValue.sortName;
        sortNameHasBeenSet = true;
        this.description = otherValue.description;
        descriptionHasBeenSet = true;
        this.groupId = otherValue.groupId;
        groupIdHasBeenSet = true;
        this.owner = otherValue.owner;
        ownerHasBeenSet = true;
        this.modifiedBy = otherValue.modifiedBy;
        modifiedByHasBeenSet = true;
        this.location = otherValue.location;
        locationHasBeenSet = true;
        this.id = otherValue.id;
        idHasBeenSet = true;
        this.mTime = otherValue.mTime;
        mTimeHasBeenSet = true;
        this.cTime = otherValue.cTime;
        cTimeHasBeenSet = true;
        // TODO Clone is better no ?
        this.ServiceType = otherValue.ServiceType;
        ServiceTypeHasBeenSet = true;

    }

    public String getName()
    {
        return this.name;
    }

    public void setName( String name )
    {
        this.name = name;
        nameHasBeenSet = true;

    }

    public boolean nameHasBeenSet(){
        return nameHasBeenSet;
    }
    public String getSortName()
    {
        return this.sortName;
    }

    public void setSortName( String sortName )
    {
        this.sortName = sortName;
        sortNameHasBeenSet = true;

    }

    public boolean sortNameHasBeenSet(){
        return sortNameHasBeenSet;
    }
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription( String description )
    {
        this.description = description;
        descriptionHasBeenSet = true;

    }

    public boolean descriptionHasBeenSet(){
        return descriptionHasBeenSet;
    }
    public Integer getGroupId()
    {
        return this.groupId;
    }

    public void setGroupId( Integer groupId )
    {
        this.groupId = groupId;
        groupIdHasBeenSet = true;

    }

    public boolean groupIdHasBeenSet(){
        return groupIdHasBeenSet;
    }
    public String getOwner()
    {
        return this.owner;
    }

    public void setOwner( String owner )
    {
        this.owner = owner;
        ownerHasBeenSet = true;

    }

    public boolean ownerHasBeenSet(){
        return ownerHasBeenSet;
    }
    public String getModifiedBy()
    {
        return this.modifiedBy;
    }

    public void setModifiedBy( String modifiedBy )
    {
        this.modifiedBy = modifiedBy;
        modifiedByHasBeenSet = true;

    }

    public boolean modifiedByHasBeenSet(){
        return modifiedByHasBeenSet;
    }
    public String getLocation()
    {
        return this.location;
    }

    public void setLocation( String location )
    {
        this.location = location;
        locationHasBeenSet = true;

    }

    public boolean locationHasBeenSet(){
        return locationHasBeenSet;
    }
    public Integer getId()
    {
        return this.id;
    }

    public void setId( Integer id )
    {
        this.id = id;
        idHasBeenSet = true;

    }

    public boolean idHasBeenSet(){
        return idHasBeenSet;
    }
    public Long getMTime()
    {
        return this.mTime;
    }

    public void setMTime( Long mTime )
    {
        this.mTime = mTime;
        mTimeHasBeenSet = true;

    }

    public boolean mTimeHasBeenSet(){
        return mTimeHasBeenSet;
    }
    public Long getCTime()
    {
        return this.cTime;
    }

    public void setCTime( Long cTime )
    {
        this.cTime = cTime;
        cTimeHasBeenSet = true;

    }

    public boolean cTimeHasBeenSet(){
        return cTimeHasBeenSet;
    }

    public ServiceTypeValue getServiceType()
    {
        return this.ServiceType;
    }
    public void setServiceType( ServiceTypeValue ServiceType )
    {
        this.ServiceType = ServiceType;
        ServiceTypeHasBeenSet = true;
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer("{");

        str.append("name=" + getName() + " sortName=" + getSortName()
                + " description=" + getDescription() + " groupId="
                + getGroupId() + " owner=" + getOwner() + " modifiedBy="
                + getModifiedBy() + " location=" + getLocation() + " id="
                + getId() + " mTime=" + getMTime() + " cTime=" + getCTime());
        str.append('}');

        return(str.toString());
    }

    /**
     * A Value object have an identity if its attributes making its Primary Key
     * has all been set.  One object without identity is never equal to any other
     * object.
     *
     * @return true if this instance have an identity.
     */
    protected boolean hasIdentity()
    {
        boolean ret = true;
        ret = ret && idHasBeenSet;
        return ret;
    }

    public boolean equals(Object other)
    {
        if ( ! hasIdentity() ) return false;
        if (other instanceof ServiceClusterValue)
        {
            ServiceClusterValue that = (ServiceClusterValue) other;
            if ( ! that.hasIdentity() ) return false;
            boolean lEquals = true;
            if( this.id == null )
            {
                lEquals = lEquals && ( that.id == null );
            }
            else
            {
                lEquals = lEquals && this.id.equals( that.id );
            }

            lEquals = lEquals && isIdentical(that);

            return lEquals;
        }
        else
        {
            return false;
        }
    }

    public boolean isIdentical(Object other)
    {
        if (other instanceof ServiceClusterValue)
        {
            ServiceClusterValue that = (ServiceClusterValue) other;
            boolean lEquals = true;
            if( this.name == null )
            {
                lEquals = lEquals && ( that.name == null );
            }
            else
            {
                lEquals = lEquals && this.name.equals( that.name );
            }
            if( this.sortName == null )
            {
                lEquals = lEquals && ( that.sortName == null );
            }
            else
            {
                lEquals = lEquals && this.sortName.equals( that.sortName );
            }
            if( this.description == null )
            {
                lEquals = lEquals && ( that.description == null );
            }
            else
            {
                lEquals = lEquals && this.description.equals( that.description );
            }
            if( this.groupId == null )
            {
                lEquals = lEquals && ( that.groupId == null );
            }
            else
            {
                lEquals = lEquals && this.groupId.equals( that.groupId );
            }
            if( this.owner == null )
            {
                lEquals = lEquals && ( that.owner == null );
            }
            else
            {
                lEquals = lEquals && this.owner.equals( that.owner );
            }
            if( this.modifiedBy == null )
            {
                lEquals = lEquals && ( that.modifiedBy == null );
            }
            else
            {
                lEquals = lEquals && this.modifiedBy.equals( that.modifiedBy );
            }
            if( this.location == null )
            {
                lEquals = lEquals && ( that.location == null );
            }
            else
            {
                lEquals = lEquals && this.location.equals( that.location );
            }
            if( this.mTime == null )
            {
                lEquals = lEquals && ( that.mTime == null );
            }
            else
            {
                lEquals = lEquals && this.mTime.equals( that.mTime );
            }
            if( this.cTime == null )
            {
                lEquals = lEquals && ( that.cTime == null );
            }
            else
            {
                lEquals = lEquals && this.cTime.equals( that.cTime );
            }
            if( this.ServiceType == null )
            {
                lEquals = lEquals && ( that.ServiceType == null );
            }
            else
            {
                lEquals = lEquals && this.ServiceType.equals( that.ServiceType );
            }

            return lEquals;
        }
        else
        {
            return false;
        }
    }

    public int hashCode(){
        int result = 17;
        result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

        result = 37*result + ((this.sortName != null) ? this.sortName.hashCode() : 0);

        result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

        result = 37*result + ((this.groupId != null) ? this.groupId.hashCode() : 0);

        result = 37*result + ((this.owner != null) ? this.owner.hashCode() : 0);

        result = 37*result + ((this.modifiedBy != null) ? this.modifiedBy.hashCode() : 0);

        result = 37*result + ((this.location != null) ? this.location.hashCode() : 0);

        result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

        result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);

        result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);

        result = 37*result + ((this.ServiceType != null) ? this.ServiceType.hashCode() : 0);
        return result;
    }

    public AppdefEntityID getEntityId() {
        // hello, butt-ugliness-who-makes-me-wince-whenever-I-see-you
        // the AppdefEntityID must be of the underlying group so we do 
        // something a little different
        return AppdefEntityID.newGroupID(
                                         ((ServiceClusterValue)this).getGroupId());
    }
}
