/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import org.hyperic.hq.appdef.server.session.Platform;

/**
 * Value object for Server.
 *
 */
public class ServerValue extends AppdefResourceValue
   implements java.io.Serializable
{
    private String sortName;
    private boolean sortNameHasBeenSet = false;
    private boolean runtimeAutodiscovery;
    private boolean runtimeAutodiscoveryHasBeenSet = false;
    private boolean wasAutodiscovered;
    private boolean wasAutodiscoveredHasBeenSet = false;
    private boolean autodiscoveryZombie;
    private boolean autodiscoveryZombieHasBeenSet = false;
    private Integer configResponseId;
    private boolean configResponseIdHasBeenSet = false;
    private String modifiedBy;
    private boolean modifiedByHasBeenSet = false;
    private String owner;
    private boolean ownerHasBeenSet = false;
    private String location;
    private boolean locationHasBeenSet = false;
    private String name;
    private boolean nameHasBeenSet = false;
    private String autoinventoryIdentifier;
    private boolean autoinventoryIdentifierHasBeenSet = false;
    private String installPath;
    private boolean installPathHasBeenSet = false;
    private String description;
    private boolean descriptionHasBeenSet = false;
    private boolean servicesAutomanaged;
    private boolean servicesAutomanagedHasBeenSet = false;
    private Integer id;
    private boolean idHasBeenSet = false;
    private Long mTime;
    private boolean mTimeHasBeenSet = false;
    private Long cTime;
    private boolean cTimeHasBeenSet = false;
    private ServerTypeValue ServerType;
    private boolean ServerTypeHasBeenSet = false;
    private Platform Platform;
    private boolean PlatformHasBeenSet = false;

    public ServerValue()
    {
    }

    public ServerValue( String sortName,boolean runtimeAutodiscovery,boolean wasAutodiscovered,boolean autodiscoveryZombie,Integer configResponseId,String modifiedBy,String owner,String location,String name,String autoinventoryIdentifier,String installPath,String description,boolean servicesAutomanaged,Integer id,Long mTime,Long cTime )
    {
        this.sortName = sortName;
        sortNameHasBeenSet = true;
        this.runtimeAutodiscovery = runtimeAutodiscovery;
        runtimeAutodiscoveryHasBeenSet = true;
        this.wasAutodiscovered = wasAutodiscovered;
        wasAutodiscoveredHasBeenSet = true;
        this.autodiscoveryZombie = autodiscoveryZombie;
        autodiscoveryZombieHasBeenSet = true;
        this.configResponseId = configResponseId;
        configResponseIdHasBeenSet = true;
        this.modifiedBy = modifiedBy;
        modifiedByHasBeenSet = true;
        this.owner = owner;
        ownerHasBeenSet = true;
        this.location = location;
        locationHasBeenSet = true;
        this.name = name;
        nameHasBeenSet = true;
        this.autoinventoryIdentifier = autoinventoryIdentifier;
        autoinventoryIdentifierHasBeenSet = true;
        this.installPath = installPath;
        installPathHasBeenSet = true;
        this.description = description;
        descriptionHasBeenSet = true;
        this.servicesAutomanaged = servicesAutomanaged;
        servicesAutomanagedHasBeenSet = true;
        this.id = id;
        idHasBeenSet = true;
        this.mTime = mTime;
        mTimeHasBeenSet = true;
        this.cTime = cTime;
        cTimeHasBeenSet = true;
    }

    //TODO Cloneable is better than this !
    public ServerValue( ServerValue otherValue )
    {
        this.sortName = otherValue.sortName;
        sortNameHasBeenSet = true;
        this.runtimeAutodiscovery = otherValue.runtimeAutodiscovery;
        runtimeAutodiscoveryHasBeenSet = true;
        this.wasAutodiscovered = otherValue.wasAutodiscovered;
        wasAutodiscoveredHasBeenSet = true;
        this.autodiscoveryZombie = otherValue.autodiscoveryZombie;
        autodiscoveryZombieHasBeenSet = true;
        this.configResponseId = otherValue.configResponseId;
        configResponseIdHasBeenSet = true;
        this.modifiedBy = otherValue.modifiedBy;
        modifiedByHasBeenSet = true;
        this.owner = otherValue.owner;
        ownerHasBeenSet = true;
        this.location = otherValue.location;
        locationHasBeenSet = true;
        this.name = otherValue.name;
        nameHasBeenSet = true;
        this.autoinventoryIdentifier = otherValue.autoinventoryIdentifier;
        autoinventoryIdentifierHasBeenSet = true;
        this.installPath = otherValue.installPath;
        installPathHasBeenSet = true;
        this.description = otherValue.description;
        descriptionHasBeenSet = true;
        this.servicesAutomanaged = otherValue.servicesAutomanaged;
        servicesAutomanagedHasBeenSet = true;
        this.id = otherValue.id;
        idHasBeenSet = true;
        this.mTime = otherValue.mTime;
        mTimeHasBeenSet = true;
        this.cTime = otherValue.cTime;
        cTimeHasBeenSet = true;
        // TODO Clone is better no ?
        this.ServerType = otherValue.ServerType;
        ServerTypeHasBeenSet = true;
        // TODO Clone is better no ?
        this.Platform = otherValue.Platform;
        PlatformHasBeenSet = true;

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
    public boolean getRuntimeAutodiscovery()
    {
        return this.runtimeAutodiscovery;
    }

    public void setRuntimeAutodiscovery( boolean runtimeAutodiscovery )
    {
        this.runtimeAutodiscovery = runtimeAutodiscovery;
        runtimeAutodiscoveryHasBeenSet = true;

    }

    public boolean runtimeAutodiscoveryHasBeenSet(){
        return runtimeAutodiscoveryHasBeenSet;
    }
    public boolean getWasAutodiscovered()
    {
        return this.wasAutodiscovered;
    }

    public void setWasAutodiscovered( boolean wasAutodiscovered )
    {
        this.wasAutodiscovered = wasAutodiscovered;
        wasAutodiscoveredHasBeenSet = true;

    }

    public boolean wasAutodiscoveredHasBeenSet(){
        return wasAutodiscoveredHasBeenSet;
    }
    public boolean getAutodiscoveryZombie()
    {
        return this.autodiscoveryZombie;
    }

    public void setAutodiscoveryZombie( boolean autodiscoveryZombie )
    {
        this.autodiscoveryZombie = autodiscoveryZombie;
        autodiscoveryZombieHasBeenSet = true;

    }

    public boolean autodiscoveryZombieHasBeenSet(){
        return autodiscoveryZombieHasBeenSet;
    }
    public Integer getConfigResponseId()
    {
        return this.configResponseId;
    }

    public void setConfigResponseId( Integer configResponseId )
    {
        this.configResponseId = configResponseId;
        configResponseIdHasBeenSet = true;

    }

    public boolean configResponseIdHasBeenSet(){
        return configResponseIdHasBeenSet;
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
    public String getAutoinventoryIdentifier()
    {
        return this.autoinventoryIdentifier;
    }

    public void setAutoinventoryIdentifier( String autoinventoryIdentifier )
    {
        this.autoinventoryIdentifier = autoinventoryIdentifier;
        autoinventoryIdentifierHasBeenSet = true;

    }

    public boolean autoinventoryIdentifierHasBeenSet(){
        return autoinventoryIdentifierHasBeenSet;
    }
    public String getInstallPath()
    {
        return this.installPath;
    }

    @Property("installpath")
    public void setInstallPath( String installPath )
    {
        this.installPath = installPath;
        installPathHasBeenSet = true;

    }

    public boolean installPathHasBeenSet(){
        return installPathHasBeenSet;
    }
    public String getDescription()
    {
        return this.description;
    }

    @Property("server.description")
    public void setDescription( String description )
    {
        this.description = description;
        descriptionHasBeenSet = true;

    }

    public boolean descriptionHasBeenSet(){
        return descriptionHasBeenSet;
    }
    public boolean getServicesAutomanaged()
    {
        return this.servicesAutomanaged;
    }

    public void setServicesAutomanaged( boolean servicesAutomanaged )
    {
        this.servicesAutomanaged = servicesAutomanaged;
        servicesAutomanagedHasBeenSet = true;

    }

    public boolean servicesAutomanagedHasBeenSet(){
        return servicesAutomanagedHasBeenSet;
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


    public ServerTypeValue getServerType()
    {
        return this.ServerType;
    }
    public void setServerType( ServerTypeValue ServerType )
    {
        this.ServerType = ServerType;
        ServerTypeHasBeenSet = true;
    }
    public Platform getPlatform()
    {
        return this.Platform;
    }
    public void setPlatform( Platform Platform )
    {
        this.Platform = Platform;
        PlatformHasBeenSet = true;
    }

    public String toString()
    {
        StringBuffer str = new StringBuffer("{");

        str.append("sortName=" + getSortName() + " runtimeAutodiscovery="
                + getRuntimeAutodiscovery() + " wasAutodiscovered="
                + getWasAutodiscovered() + " autodiscoveryZombie="
                + getAutodiscoveryZombie() + " configResponseId="
                + getConfigResponseId() + " modifiedBy=" + getModifiedBy()
                + " owner=" + getOwner() + " location=" + getLocation()
                + " name=" + getName() + " autoinventoryIdentifier="
                + getAutoinventoryIdentifier() + " installPath="
                + getInstallPath() + " description=" + getDescription()
                + " servicesAutomanaged=" + getServicesAutomanaged() + " id="
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
        if (other instanceof ServerValue)
        {
            ServerValue that = (ServerValue) other;
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
        if (other instanceof ServerValue)
        {
            ServerValue that = (ServerValue) other;
            boolean lEquals = true;
            if( this.sortName == null )
            {
                lEquals = lEquals && ( that.sortName == null );
            }
            else
            {
                lEquals = lEquals && this.sortName.equals( that.sortName );
            }
            lEquals = lEquals && this.runtimeAutodiscovery == that.runtimeAutodiscovery;
            lEquals = lEquals && this.wasAutodiscovered == that.wasAutodiscovered;
            lEquals = lEquals && this.autodiscoveryZombie == that.autodiscoveryZombie;
            if( this.configResponseId == null )
            {
                lEquals = lEquals && ( that.configResponseId == null );
            }
            else
            {
                lEquals = lEquals && this.configResponseId.equals( that.configResponseId );
            }
            if( this.modifiedBy == null )
            {
                lEquals = lEquals && ( that.modifiedBy == null );
            }
            else
            {
                lEquals = lEquals && this.modifiedBy.equals( that.modifiedBy );
            }
            if( this.owner == null )
            {
                lEquals = lEquals && ( that.owner == null );
            }
            else
            {
                lEquals = lEquals && this.owner.equals( that.owner );
            }
            if( this.location == null )
            {
                lEquals = lEquals && ( that.location == null );
            }
            else
            {
                lEquals = lEquals && this.location.equals( that.location );
            }
            if( this.name == null )
            {
                lEquals = lEquals && ( that.name == null );
            }
            else
            {
                lEquals = lEquals && this.name.equals( that.name );
            }
            if( this.autoinventoryIdentifier == null )
            {
                lEquals = lEquals && ( that.autoinventoryIdentifier == null );
            }
            else
            {
                lEquals = lEquals && this.autoinventoryIdentifier.equals( that.autoinventoryIdentifier );
            }
            if( this.installPath == null )
            {
                lEquals = lEquals && ( that.installPath == null );
            }
            else
            {
                lEquals = lEquals && this.installPath.equals( that.installPath );
            }
            if( this.description == null )
            {
                lEquals = lEquals && ( that.description == null );
            }
            else
            {
                lEquals = lEquals && this.description.equals( that.description );
            }
            lEquals = lEquals && this.servicesAutomanaged == that.servicesAutomanaged;
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
            if( this.ServerType == null )
            {
                lEquals = lEquals && ( that.ServerType == null );
            }
            else
            {
                lEquals = lEquals && this.ServerType.equals( that.ServerType );
            }
            if( this.Platform == null )
            {
                lEquals = lEquals && ( that.Platform == null );
            }
            else
            {
                lEquals = lEquals && this.Platform.equals( that.Platform );
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
        result = 37*result + ((this.sortName != null) ? this.sortName.hashCode() : 0);

        result = 37*result + (runtimeAutodiscovery ? 0 : 1);

        result = 37*result + (wasAutodiscovered ? 0 : 1);

        result = 37*result + (autodiscoveryZombie ? 0 : 1);

        result = 37*result + ((this.configResponseId != null) ? this.configResponseId.hashCode() : 0);

        result = 37*result + ((this.modifiedBy != null) ? this.modifiedBy.hashCode() : 0);

        result = 37*result + ((this.owner != null) ? this.owner.hashCode() : 0);

        result = 37*result + ((this.location != null) ? this.location.hashCode() : 0);

        result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);

        result = 37*result + ((this.autoinventoryIdentifier != null) ? this.autoinventoryIdentifier.hashCode() : 0);

        result = 37*result + ((this.installPath != null) ? this.installPath.hashCode() : 0);

        result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);

        result = 37*result + (servicesAutomanaged ? 0 : 1);

        result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);

        result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);

        result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);

        result = 37*result + ((this.ServerType != null) ? this.ServerType.hashCode() : 0);
        result = 37*result + ((this.Platform != null) ? this.Platform.hashCode() : 0);
        return result;
    }

    public AppdefEntityID getEntityId() {
        return AppdefEntityID.newServerID(getId());
    }

}
