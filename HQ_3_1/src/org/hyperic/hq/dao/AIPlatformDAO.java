package org.hyperic.hq.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;

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

public class AIPlatformDAO extends HibernateDAO
{
    private static final Log log = LogFactory.getLog(AIPlatformDAO.class);

    public AIPlatformDAO(DAOFactory f) {
        super(AIPlatform.class, f);
    }

    public AIPlatform findById(Integer id) {
        return (AIPlatform)super.findById(id);
    }

    public void save(AIPlatform entity) {
        super.save(entity);
    }

    public void remove(AIPlatform entity)
    {
        super.remove(entity);
    }

    private void fixdata(AIPlatform p)
    {
        String name = p.getName();
        if (name == null || "".equals(name.trim())) {
            p.setName(p.getFqdn());
        }
    }

    public AIPlatform create(AIPlatformValue apv)
    {
        AIPlatform p = new AIPlatform(apv);
        fixdata(p);

        // handle IP's in the value object
        // Since this is a new value object
        // I'll assume we need add all ips
        AIIpValue[] newIPs = apv.getAIIpValues();
        Collection ipSet = new HashSet();
        p.setAIIps(ipSet);

        for ( int i=0; i<newIPs.length; i++ ) {
            AIIpValue ipVal = newIPs[i];
            AIIp ip = new AIIp(ipVal);
            ip.setAIPlatform(p);
            ipSet.add(ip);
        }

        // handle Server's in the value object
        // Since this is a new value object
        // I'll assume we need add all servers
        AIServerValue[] newServers = apv.getAIServerValues();
        Collection serverSet = new HashSet();
        p.setAIServers(serverSet);

        // XXX hack around bug in xcraplet's generated
        // removeAllXXX methods (they actually don't remove anything)
        // The AIQueueManagerEJBImpl.queue method relies on this working.
        HashSet xdocletServerHackSet = new HashSet();
        for (int i=0; i<newServers.length; i++) {
            AIServerValue serverVal = newServers[i];
            AIServer s = new AIServer(serverVal);
            s.setAIPlatform(p);
            serverSet.add(s);
        }
        save(p);
        return p;
    }

    public Collection findAllNotIgnored()
    {
        String sql="from AIPlatform where ignored=false and " +
                   "lastApproved < modifiedTime " +
                   "order by name";
        return getSession().createQuery(sql).list();
    }

    public Collection findAllNotIgnoredIncludingProcessed()
    {
        String sql="from AIPlatform where ignored=false order by name";
        return getSession().createQuery(sql).list();
    }

    public Collection findAllIncludingProcessed()
    {
        String sql="from AIPlatform order by name";
        return getSession().createQuery(sql).list();
    }

    public Collection findByFQDN(String fqdn)
    {
        String sql="from AIPlatform where fqdn=?";
        return getSession().createQuery(sql)
            .setString(0, fqdn)
            .list();
    }

    public AIPlatform findByCertDN(String dn)
    {
        String sql="from AIPlatform where certdn=?";
        return (AIPlatform)getSession().createQuery(sql)
            .setString(0, dn)
            .uniqueResult();
    }

    public AIPlatform findByAgentToken(String token)
    {
        String sql="from AIPlatform where agentToken=?";
        return (AIPlatform)getSession().createQuery(sql)
            .setString(0, token)
            .uniqueResult();
    }

    public AIPlatform findByName(String name)
    {
        String sql="from AIPlatform where lower(name)=?";
        return (AIPlatform)getSession().createQuery(sql)
            .setString(0, name.toUpperCase())
            .uniqueResult();
    }

    /**
     * legacy EJB method
     *
     * @param aiplatform The new AI data to update this platform with.
     * @param updateServers If true, servers will be updated too.
     */
    public void updateQueueState(AIPlatform aip,
                                 AIPlatformValue aiplatform,
                                 boolean updateServers,
                                 boolean isApproval,
                                 boolean isReport)
    {
        // reassociate platform
        aip = findById(aip.getId());

        long nowTime = System.currentTimeMillis();
        aip.setFqdn(aiplatform.getFqdn());
        aip.setName(aiplatform.getName());
        aip.setPlatformTypeName(aiplatform.getPlatformTypeName());
        aip.setAgentToken(aiplatform.getAgentToken());
        aip.setQueueStatus(aiplatform.getQueueStatus());
        aip.setDescription(aiplatform.getDescription());
        aip.setDiff(aiplatform.getDiff());
        aip.setCpuCount(aiplatform.getCpuCount());
        aip.setCustomProperties(aiplatform.getCustomProperties());
        aip.setProductConfig(aiplatform.getProductConfig());
        aip.setMeasurementConfig(aiplatform.getMeasurementConfig());
        aip.setControlConfig(aiplatform.getControlConfig());

        if (isReport || isApproval) {
            if (isApproval) {
                aiplatform.setLastApproved(new Long(nowTime + 1));
            } else {
                Long lastApproved = aiplatform.getLastApproved();
                if (lastApproved == null) lastApproved = new Long(0);
                aiplatform.setLastApproved(lastApproved);
            }
            aip.setLastApproved(aiplatform.getLastApproved());
        }

        // Sanitize name
        if (aip.getName() == null) {
            aip.setName(aip.getFqdn());
        }
        updateIpSet(aip, aiplatform);
        if (updateServers) {
            updateServerSet(aip, aiplatform);
        }
        save(aip);
    }

    private void updateIpSet (AIPlatform p, AIPlatformValue aiplatform)
    {
        List newIPs = new ArrayList();
        newIPs.addAll(Arrays.asList(aiplatform.getAIIpValues()));
        Collection ipSet = p.getAIIps();
        Iterator i = ipSet.iterator();
        while ( i.hasNext() ) {
            AIIp qip = (AIIp) i.next();
            AIIpValue aiip = findAndRemoveAIIp(newIPs, qip.getAddress());
            if ( aiip == null ) {
                i.remove();
            } else {
                boolean qIgnored = qip.getIgnored();
                qip.setAIIpValue(aiip);
                qip.setIgnored(qIgnored);
            }
        }

        // Add remaining IPs
        i = newIPs.iterator();
        while ( i.hasNext() ) {
            AIIpValue aiip = (AIIpValue) i.next();
            AIIp ip = new AIIp();
            ip.setAIIpValue(aiip);
            ip.setAIPlatform(p);
            ipSet.add(ip);
        }
    }

    private AIIpValue findAndRemoveAIIp (List ips, String addr) {
        AIIpValue aiip;
        for (int i=0; i<ips.size(); i++) {
            aiip = (AIIpValue) ips.get(i);
            if (aiip.getAddress().equals(addr)) {
                ips.remove(i);
                return aiip;
            }
        }
        return null;
    }

    private void updateServerSet(AIPlatform p, AIPlatformValue aiplatform)
    {
        List newServers = new ArrayList();
        newServers.addAll(Arrays.asList(aiplatform.getAIServerValues()));
        Collection serverSet = p.getAIServers();
        Iterator i = serverSet.iterator();
        while (i.hasNext()) {
            AIServer qserver = (AIServer) i.next();
            AIServerValue aiserver
                = findAndRemoveAIServer(newServers,
                                        qserver.getAutoinventoryIdentifier());

            if ( aiserver == null ) {
                i.remove();

            } else {
                // keep the user specified ignored value
                boolean qIgnored = qserver.getIgnored();
                qserver.setAIServerValue(aiserver);
                qserver.setIgnored(qIgnored);
            }
        }

        i = newServers.iterator();
        while (i.hasNext()) {
            AIServerValue aiserver = (AIServerValue) i.next();
            AIServer ais = new AIServer();
            ais.setAIServerValue(aiserver);
            p.addAIServer(ais);
        }
    }

    private AIServerValue findAndRemoveAIServer (List servers, String aiid) {
        AIServerValue aiserver;
        for (int i=0; i<servers.size(); i++) {
            aiserver = (AIServerValue) servers.get(i);
            if ( aiserver.getAutoinventoryIdentifier().equals(aiid) ) {
                servers.remove(i);
                return aiserver;
            }
        }
        return null;
    }
}
