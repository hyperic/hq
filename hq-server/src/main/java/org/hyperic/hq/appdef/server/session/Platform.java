package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.HashSet;

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
        // _platformValue.setSortName(getSortName());
        // _platformValue.setCommentText(getCommentText());
        // _platformValue.setModifiedBy(getModifiedBy());
        // _platformValue.setOwner(getOwner());
        // _platformValue.setCertdn(getCertdn());
        // _platformValue.setFqdn(getFqdn());
        // _platformValue.setName(getName());
        // _platformValue.setLocation(getLocation());
        // _platformValue.setDescription(getDescription());
        // _platformValue.setCpuCount(getCpuCount());
        // _platformValue.setId(getId());
        // _platformValue.setMTime(getMTime());
        // _platformValue.setCTime(getCTime());
        // _platformValue.removeAllIpValues();
        // Iterator iIpValue = getIps().iterator();
        // while (iIpValue.hasNext()){
        // _platformValue.addIpValue( ((Ip)iIpValue.next()).getIpValue() );
        // }
        // _platformValue.cleanIpValue();
        // if (getPlatformType() != null)
        // _platformValue.setPlatformType(
        // getPlatformType().getPlatformTypeValue());
        // else
        // _platformValue.setPlatformType( null );
        // if (getAgent() != null) {
        // // Make sure that the agent is loaded
        // getAgent().getAddress();
        // _platformValue.setAgent(getAgent());
        // }
        // else
        // _platformValue.setAgent(null);
        return platformValue;
    }

}
