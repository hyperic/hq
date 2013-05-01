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

import java.util.HashSet;
import java.util.Set;

/**
 * Value object for Platform.
 *
 */
public class PlatformValue extends AppdefResourceValue
    implements java.io.Serializable, Cloneable
{
    private String sortName;
    private boolean sortNameHasBeenSet = false;
    private String commentText;
    private boolean commentTextHasBeenSet = false;
    private String modifiedBy;
    private boolean modifiedByHasBeenSet = false;
    private String owner;
    private boolean ownerHasBeenSet = false;
    private Integer configResponseId;
    private boolean configResponseIdHasBeenSet = false;
    private String certdn;
    private boolean certdnHasBeenSet = false;
    private String fqdn;
    private boolean fqdnHasBeenSet = false;
    private String name;
    private boolean nameHasBeenSet = false;
    private String location;
    private boolean locationHasBeenSet = false;
    private String description;
    private boolean descriptionHasBeenSet = false;
    private Integer cpuCount;
    private boolean cpuCountHasBeenSet = false;
    private Integer id;
    private boolean idHasBeenSet = false;
    private Long mTime;
    private boolean mTimeHasBeenSet = false;
    private Long cTime;
    private boolean cTimeHasBeenSet = false;
    private Set<IpValue> IpValues = new HashSet<IpValue>();
    private PlatformTypeValue PlatformType;
    private boolean PlatformTypeHasBeenSet = false;
    private org.hyperic.hq.appdef.Agent Agent;
    private boolean AgentHasBeenSet = false;
    protected Set<IpValue> addedIpValues = new HashSet<IpValue>();
    protected Set<IpValue> removedIpValues = new HashSet<IpValue>();
    protected Set<IpValue> updatedIpValues = new HashSet<IpValue>();


    public PlatformValue()
    {
    }

    public PlatformValue( String sortName,String commentText,String modifiedBy,String owner,Integer configResponseId,String certdn,String fqdn,String name,String location,String description,Integer cpuCount,Integer id,Long mTime,Long cTime )
    {
        this.sortName = sortName;
        sortNameHasBeenSet = true;
        this.commentText = commentText;
        commentTextHasBeenSet = true;
        this.modifiedBy = modifiedBy;
        modifiedByHasBeenSet = true;
        this.owner = owner;
        ownerHasBeenSet = true;
        this.configResponseId = configResponseId;
        configResponseIdHasBeenSet = true;
        this.certdn = certdn;
        certdnHasBeenSet = true;
        this.fqdn = fqdn;
        fqdnHasBeenSet = true;
        this.name = name;
        nameHasBeenSet = true;
        this.location = location;
        locationHasBeenSet = true;
        this.description = description;
        descriptionHasBeenSet = true;
        this.cpuCount = cpuCount;
        cpuCountHasBeenSet = true;
        this.id = id;
        idHasBeenSet = true;
        this.mTime = mTime;
        mTimeHasBeenSet = true;
        this.cTime = cTime;
        cTimeHasBeenSet = true;
    }

    
    @Override
    public Object clone() throws CloneNotSupportedException {
    	PlatformValue clonedPlatformValue = new PlatformValue();
    	
    	clonedPlatformValue.sortName = this.sortName;
    	clonedPlatformValue.sortNameHasBeenSet = true;
    	
    	clonedPlatformValue.commentText = this.commentText;
    	clonedPlatformValue.commentTextHasBeenSet = true;
    	
    	clonedPlatformValue.modifiedBy = this.modifiedBy;
    	clonedPlatformValue.modifiedByHasBeenSet = true;
    	
    	clonedPlatformValue.owner = this.owner;
    	clonedPlatformValue.ownerHasBeenSet = true;
    	
    	clonedPlatformValue.configResponseId = this.configResponseId;
    	clonedPlatformValue.configResponseIdHasBeenSet = true;
    	
    	clonedPlatformValue.certdn = this.certdn;
    	clonedPlatformValue.certdnHasBeenSet = true;
    	
    	clonedPlatformValue.fqdn = this.fqdn;
    	clonedPlatformValue.fqdnHasBeenSet = true;
    	
    	clonedPlatformValue.name = this.name;
    	clonedPlatformValue.nameHasBeenSet = true;
    	
    	clonedPlatformValue.location = this.location;
    	clonedPlatformValue.locationHasBeenSet = true;
    	
    	clonedPlatformValue.description = this.description;
    	clonedPlatformValue.descriptionHasBeenSet = true;
    	
    	clonedPlatformValue.cpuCount = this.cpuCount;
    	clonedPlatformValue.cpuCountHasBeenSet = true;
    	
    	clonedPlatformValue.id = this.id;
    	clonedPlatformValue.idHasBeenSet = true;
    	
    	clonedPlatformValue.mTime = this.mTime;
    	clonedPlatformValue.mTimeHasBeenSet = true;
    	
    	clonedPlatformValue.IpValues = new HashSet<IpValue>();
    	for (IpValue ip : this.IpValues)
    		clonedPlatformValue.addIpValue(new IpValue(ip));
    	
    	clonedPlatformValue.PlatformType = new PlatformTypeValue(this.PlatformType);
    	clonedPlatformValue.PlatformTypeHasBeenSet = true;
    	
    	//TODO: clone the agent?
    	clonedPlatformValue.Agent = this.Agent;
    	clonedPlatformValue.AgentHasBeenSet = true;
    	
    	return clonedPlatformValue;
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
    public String getCommentText()
    {
        return this.commentText;
    }

    @Property("platform.comment_text")
    public void setCommentText( String commentText )
    {
        this.commentText = commentText;
        commentTextHasBeenSet = true;

    }

    public boolean commentTextHasBeenSet(){
        return commentTextHasBeenSet;
    }
    @Override
	public String getModifiedBy()
    {
        return this.modifiedBy;
    }

    @Override
	public void setModifiedBy( String modifiedBy )
    {
        this.modifiedBy = modifiedBy;
        modifiedByHasBeenSet = true;

    }

    public boolean modifiedByHasBeenSet(){
        return modifiedByHasBeenSet;
    }
    @Override
	public String getOwner()
    {
        return this.owner;
    }

    @Override
	public void setOwner( String owner )
    {
        this.owner = owner;
        ownerHasBeenSet = true;

    }

    public boolean ownerHasBeenSet(){
        return ownerHasBeenSet;
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
    public String getCertdn()
    {
        return this.certdn;
    }

    public void setCertdn( String certdn )
    {
        this.certdn = certdn;
        certdnHasBeenSet = true;

    }

    public boolean certdnHasBeenSet(){
        return certdnHasBeenSet;
    }
    public String getFqdn()
    {
        return this.fqdn;
    }

    @Property("platform.fqdn") 
    public void setFqdn( String fqdn )
    {
        this.fqdn = fqdn;
        fqdnHasBeenSet = true;

    }

    public boolean fqdnHasBeenSet(){
        return fqdnHasBeenSet;
    }
    @Override
	public String getName()
    {
        return this.name;
    }

    @Override
	public void setName( String name )
    {
        this.name = name;
        nameHasBeenSet = true;

    }

    public boolean nameHasBeenSet(){
        return nameHasBeenSet;
    }
    @Override
	public String getLocation()
    {
        return this.location;
    }

    @Property("platform.location")
    @Override
	public void setLocation( String location )
    {
        this.location = location;
        locationHasBeenSet = true;

    }

    public boolean locationHasBeenSet(){
        return locationHasBeenSet;
    }
    @Override
	public String getDescription()
    {
        return this.description;
    }

    @Property("platform.description") 
    @Override
	public void setDescription( String description )
    {
        this.description = description;
        descriptionHasBeenSet = true;

    }

    public boolean descriptionHasBeenSet(){
        return descriptionHasBeenSet;
    }
    public Integer getCpuCount()
    {
        return this.cpuCount;
    }

    public void setCpuCount( Integer cpuCount )
    {
        this.cpuCount = cpuCount;
        cpuCountHasBeenSet = true;

    }

    public boolean cpuCountHasBeenSet(){
        return cpuCountHasBeenSet;
    }
    @Override
	public Integer getId()
    {
        return this.id;
    }

    @Override
	public void setId( Integer id )
    {
        this.id = id;
        idHasBeenSet = true;
    }

    public boolean idHasBeenSet(){
        return idHasBeenSet;
    }
    @Override
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
    @Override
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

    public java.util.Collection<IpValue> getAddedIpValues() { return addedIpValues; }
    public java.util.Collection<IpValue> getRemovedIpValues() { return removedIpValues; }
    public java.util.Collection<IpValue> getUpdatedIpValues() { return updatedIpValues; }

    public org.hyperic.hq.appdef.shared.IpValue[] getIpValues()
    {
        return this.IpValues.toArray(new org.hyperic.hq.appdef.shared.IpValue[IpValues.size()]);
    }

    public void addIpValue(org.hyperic.hq.appdef.shared.IpValue added)
    {
        this.IpValues.add(added);
        if ( ! this.addedIpValues.contains(added))
            this.addedIpValues.add(added);
    }

    public void removeIpValue(org.hyperic.hq.appdef.shared.IpValue removed)
    {
        this.IpValues.remove(removed);
        this.removedIpValues.add(removed);
        if (this.addedIpValues.contains(removed))
            this.addedIpValues.remove(removed);
        if (this.updatedIpValues.contains(removed))
            this.updatedIpValues.remove(removed);
    }

    public void removeAllIpValues()
    {
        // DOH. Clear the collection - javier 2/24/03
        this.IpValues.clear();
    }

    public void updateIpValue(org.hyperic.hq.appdef.shared.IpValue updated)
    {
        if ( ! this.updatedIpValues.contains(updated))
            this.updatedIpValues.add(updated);
    }

    public void cleanIpValue(){
        this.addedIpValues = new HashSet<IpValue>();
        this.removedIpValues = new HashSet<IpValue>();
        this.updatedIpValues = new HashSet<IpValue>();
    }

    public void copyIpValuesFrom(org.hyperic.hq.appdef.shared.PlatformValue from)
    {
        // TODO Clone the List ????
        this.IpValues = from.IpValues;
    }

    public org.hyperic.hq.appdef.shared.PlatformTypeValue getPlatformType()
    {
        return this.PlatformType;
    }
    public void setPlatformType( org.hyperic.hq.appdef.shared.PlatformTypeValue PlatformType )
    {
        this.PlatformType = PlatformType;
        PlatformTypeHasBeenSet = true;
    }
    public org.hyperic.hq.appdef.Agent getAgent()
    {
        return Agent;
    }
    public void setAgent( org.hyperic.hq.appdef.Agent Agent )
    {
        this.Agent = Agent;
        AgentHasBeenSet = true;
    }

    @Override
	public String toString() {
        StringBuffer str = new StringBuffer("{");
        str.append("sortName=" + getSortName() + " commentText="
                + getCommentText() + " modifiedBy=" + getModifiedBy()
                + " owner=" + getOwner() + " configResponseId="
                + getConfigResponseId() + " certdn=" + getCertdn() + " fqdn="
                + getFqdn() + " name=" + getName() + " location="
                + getLocation() + " description=" + getDescription()
                + " cpuCount=" + getCpuCount() + " id=" + getId() + " mTime="
                + getMTime() + " cTime=" + getCTime());
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

    @Override
	public boolean equals(Object other)
    {
        if ( ! hasIdentity() ) return false;
        if (other instanceof PlatformValue)
        {
            PlatformValue that = (PlatformValue) other;
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
        if (other instanceof PlatformValue)
        {
            PlatformValue that = (PlatformValue) other;
            boolean lEquals = true;
            if( this.sortName == null )
            {
                lEquals = lEquals && ( that.sortName == null );
            }
            else
            {
                lEquals = lEquals && this.sortName.equals( that.sortName );
            }
            if( this.commentText == null )
            {
                lEquals = lEquals && ( that.commentText == null );
            }
            else
            {
                lEquals = lEquals && this.commentText.equals( that.commentText );
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
            if( this.configResponseId == null )
            {
                lEquals = lEquals && ( that.configResponseId == null );
            }
            else
            {
                lEquals = lEquals && this.configResponseId.equals( that.configResponseId );
            }
            if( this.certdn == null )
            {
                lEquals = lEquals && ( that.certdn == null );
            }
            else
            {
                lEquals = lEquals && this.certdn.equals( that.certdn );
            }
            if( this.fqdn == null )
            {
                lEquals = lEquals && ( that.fqdn == null );
            }
            else
            {
                lEquals = lEquals && this.fqdn.equals( that.fqdn );
            }
            if( this.name == null )
            {
                lEquals = lEquals && ( that.name == null );
            }
            else
            {
                lEquals = lEquals && this.name.equals( that.name );
            }
            if( this.location == null )
            {
                lEquals = lEquals && ( that.location == null );
            }
            else
            {
                lEquals = lEquals && this.location.equals( that.location );
            }
            if( this.description == null )
            {
                lEquals = lEquals && ( that.description == null );
            }
            else
            {
                lEquals = lEquals && this.description.equals( that.description );
            }
            if( this.cpuCount == null )
            {
                lEquals = lEquals && ( that.cpuCount == null );
            }
            else
            {
                lEquals = lEquals && this.cpuCount.equals( that.cpuCount );
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
            if( this.getIpValues() == null )
            {
                lEquals = lEquals && ( that.getIpValues() == null );
            }
            else
            {
                // XXX Covalent Custom - dont compare the arrays, as order is not significant. ever.    
                // - javier 7/16/03
                java.util.Collection cmr1 = java.util.Arrays.asList(this.getIpValues());
                java.util.Collection cmr2 = java.util.Arrays.asList(that.getIpValues());
                // lEquals = lEquals && java.util.Arrays.equals(this.getIpValues() , that.getIpValues()) ;
                lEquals = lEquals && cmr1.containsAll(cmr2);
            }
            if( this.PlatformType == null )
            {
                lEquals = lEquals && ( that.PlatformType == null );
            }
            else
            {
                lEquals = lEquals && this.PlatformType.equals( that.PlatformType );
            }
            if( this.Agent == null )
            {
                lEquals = lEquals && ( that.Agent == null );
            }
            else
            {
                lEquals = lEquals && this.Agent.equals( that.Agent );
            }

            return lEquals;
        }
        else
        {
            return false;
        }
    }

    private Integer getIpValuesHash(IpValue[] ips) {
        int hash = 17;
        for (IpValue ip : ips) {
            hash = 37*(ip.hashCode() + hash);
        }
        return hash;
    }

    @Override
	public int hashCode(){
        int result = 17;
        result = 37*result + ((this.sortName != null) ? this.sortName.hashCode() : 0);
        result = 37*result + ((this.commentText != null) ? this.commentText.hashCode() : 0);
        result = 37*result + ((this.modifiedBy != null) ? this.modifiedBy.hashCode() : 0);
        result = 37*result + ((this.owner != null) ? this.owner.hashCode() : 0);
        result = 37*result + ((this.configResponseId != null) ? this.configResponseId.hashCode() : 0);
        result = 37*result + ((this.certdn != null) ? this.certdn.hashCode() : 0);
        result = 37*result + ((this.fqdn != null) ? this.fqdn.hashCode() : 0);
        result = 37*result + ((this.name != null) ? this.name.hashCode() : 0);
        result = 37*result + ((this.location != null) ? this.location.hashCode() : 0);
        result = 37*result + ((this.description != null) ? this.description.hashCode() : 0);
        result = 37*result + ((this.cpuCount != null) ? this.cpuCount.hashCode() : 0);
        result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);
        result = 37*result + ((this.mTime != null) ? this.mTime.hashCode() : 0);
        result = 37*result + ((this.cTime != null) ? this.cTime.hashCode() : 0);
        final IpValue[] ips = getIpValues();
        result = 37*result + ((ips != null) ? this.getIpValuesHash(ips) : 0);
        result = 37*result + ((this.PlatformType != null) ? this.PlatformType.hashCode() : 0);
        result = 37*result + ((this.Agent != null) ? this.Agent.hashCode() : 0);
        return result;
    }

    @Override
	public AppdefEntityID getEntityId() {
        return AppdefEntityID.newPlatformID(getId());       
    }
}
