package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.PlatformValue;

public class Platform
    extends AppdefResource {

    private Integer cpuCount;

    private String fqdn;

    private PlatformType platformType;

    private Agent agent;

    private String certdn;

    private String commentText;

    private Collection<Ip> ips = new HashSet<Ip>();

    private Collection<Server> servers = new HashSet<Server>();

    public Integer getCpuCount() {
        return cpuCount;
    }

    public void setCpuCount(Integer cpuCount) {
        this.cpuCount = cpuCount;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public Collection<Ip> getIps() {
        return ips;
    }
    
    public void addIp(Ip ip) {
        this.ips.add(ip);
    }

    public Collection<Server> getServers() {
        return servers;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public String getCertdn() {
        return certdn;
    }

    public void setCertdn(String certdn) {
        this.certdn = certdn;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    @Override
    public AppdefResourceType getAppdefResourceType() {
        return platformType;
    }

    @Override
    public AppdefResourceValue getAppdefResourceValue() {
        return getPlatformValue();
    }

    @Override
    protected String _getAuthzOp(String op) {
        // TODO Auto-generated method stub
        return null;
    }

    public AppdefEntityID getEntityId() { // TODO remove this - entity ID has no
                                          // place in new Resource model
        return AppdefEntityID.newPlatformID(getId());
    }

    public PlatformValue getPlatformValue() {
        PlatformValue platformValue = new PlatformValue();
         platformValue.setSortName(getSortName());
         platformValue.setCommentText(getCommentText());
         platformValue.setModifiedBy(getModifiedBy());
         platformValue.setOwner(getOwner());
         platformValue.setCertdn(getCertdn());
         platformValue.setFqdn(getFqdn());
         platformValue.setName(getName());
         platformValue.setLocation(getLocation());
         platformValue.setDescription(getDescription());
         platformValue.setCpuCount(getCpuCount());
         platformValue.setId(getId());
         platformValue.setMTime(getMTime());
         platformValue.setCTime(getCTime());
         platformValue.removeAllIpValues();
         Iterator iIpValue = getIps().iterator();
         while (iIpValue.hasNext()){
             platformValue.addIpValue( ((Ip)iIpValue.next()).getIpValue() );
         }
         platformValue.cleanIpValue();
         if (getPlatformType() != null)
             platformValue.setPlatformType(
         getPlatformType().getPlatformTypeValue());
         else
             platformValue.setPlatformType( null );
         if (getAgent() != null) {
         // Make sure that the agent is loaded
         getAgent().getAddress();
             platformValue.setAgent(getAgent());
         }
         else
             platformValue.setAgent(null);
        return platformValue;
    }
    
    private String getOwner() {
        return getResource() != null && getResource().getOwner() != null ?
                                                                          getResource().getOwner().getName() : "";
    }
    
    /**
     * Compare this entity to a value object
     * (legacy code from entity bean)
     * @return true if this platform is the same as the one in the val obj
     */
    public boolean matchesValueObject(PlatformValue obj) {
        boolean matches;
 
        matches = super.matchesValueObject(obj) ;
        matches &=
            (this.getName() != null ? this.getName().equals(obj.getName())
                : (obj.getName() == null)) ;
        matches &=
            (this.getDescription() != null ?
                this.getDescription().equals(obj.getDescription())
                : (obj.getDescription() == null)) ;
        matches &=
            (this.getCertdn() != null ? this.getCertdn().equals(obj.getCertdn())
                : (obj.getCertdn() == null)) ;
        matches &=
            (this.getCommentText() != null ?
                this.getCommentText().equals(obj.getCommentText())
                : (obj.getCommentText() == null)) ;
        matches &=
            (this.getCpuCount() != null ?
                this.getCpuCount().equals(obj.getCpuCount())
                : (obj.getCpuCount() == null)) ;
        matches &=
            (this.getFqdn() != null ? this.getFqdn().equals(obj.getFqdn())
                : (obj.getFqdn() == null)) ;
        matches &=
            (this.getLocation() != null ?
                this.getLocation().equals(obj.getLocation())
                : (obj.getLocation() == null)) ;
        // now for the IP's
        // if there's any in the addedIp's collection, it was messed with
        // which means the match fails
        matches &=
            (obj.getAddedIpValues().size() == 0) ;
        matches &=
            (obj.getRemovedIpValues().size() == 0) ;
        // check to see if we have changed the agent
        matches &=
            (this.getAgent() != null ? this.getAgent().equals(obj.getAgent())
                : (obj.getAgent() == null));
        return matches;
    }

}
