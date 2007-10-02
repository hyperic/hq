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

package org.hyperic.hq.appdef.shared;

import javax.ejb.FinderException;

import org.apache.commons.logging.Log;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

/**
 * A utility for converting value objects from AI to appdef.
 */
public class AIConversionUtil {

    private AIConversionUtil () {}

    /**
     * Merge an AIIpValue into an existing IpValue.
     * @param aiip The AIIpValue object.
     * @param ip The IpValue object representing an existing IP.
     * @return the updated IpValue object.
     */
    public static IpValue mergeAIIpIntoIp(AIIpValue aiip, IpValue ip) {
        ip.setAddress   (aiip.getAddress());
        ip.setNetmask   (aiip.getNetmask());
        ip.setMACAddress(aiip.getMACAddress());
        return ip;
    }

    /**
     * Generate an IpValue given an AIIpValue.
     * @param aiip The AIIpValue object.
     * @return an equivalent IpValue object.
     */
    public static IpValue convertAIIpToIp(AIIpValue aiip) {
        IpValue ip = new IpValue();
        ip.setAddress   (aiip.getAddress());
        ip.setNetmask   (aiip.getNetmask());
        ip.setMACAddress(aiip.getMACAddress());
        return ip;
    }

    /**
     * Merge an AIServerValue into an existing ServerValue.
     * @param aiserver The AIServerValue object.
     * @param server The ServerValue object representing an existing server.
     * @return an equivalent ServerValue object.
     */
    public static ServerValue mergeAIServerIntoServer(AIServerValue aiserver,
                                                      ServerValue server) {
        // some plugins cheat and send null attributes on scans. dont replace if null
        if(aiserver.getDescription() != null) server.setDescription(aiserver.getDescription());
        if(aiserver.getName() != null) server.setName(aiserver.getName());
        if(aiserver.getInstallPath() != null) server.setInstallPath(aiserver.getInstallPath());
        if(aiserver.getAutoinventoryIdentifier() != null) 
            server.setAutoinventoryIdentifier(aiserver.getAutoinventoryIdentifier());
        server.setServicesAutomanaged(aiserver.getServicesAutomanaged());
        return server;
    }

    /**
     * Merge an AIServiceValue into an existing ServiceValue.
     * @param aiservice The AIServiceValue object.
     * @param service The ServiceValue object representing an existing service.
     * @return an equivalent ServiceValue object.
     */
    public static ServiceValue mergeAIServiceIntoService(AIServiceValue aiservice,
                                                         ServiceValue service) {
        if(aiservice.getDescription() != null) 
            service.setDescription(aiservice.getDescription());
        if(aiservice.getName() != null) 
            service.setName(aiservice.getName());
        return service;
    }

    /**
     * Generate an ServerValue given an AIServerValue.
     * @param aiserver The AIServerValue object.
     * @return an equivalent ServerValue object.
     */
    public static ServerValue convertAIServerToServer(AIServerValue aiserver,
                                                      ServerManagerLocal serverMgr)
        throws FinderException {

        ServerTypeValue stValue;
        stValue = serverMgr.findServerTypeByName(aiserver.getServerTypeName());

        ServerValue server = new ServerValue();
        server.setDescription(aiserver.getDescription());
        server.setName       (aiserver.getName());
        server.setInstallPath(aiserver.getInstallPath());
        server.setAutoinventoryIdentifier(aiserver.getAutoinventoryIdentifier());
        server.setServerType(stValue);

        if ( server.getName() == null ) {
            server.setName(aiserver.getServerTypeName()
                           + " (" + System.currentTimeMillis() + ")");
        }
        return server;
    }

    /**
     * Generate a ServiceValue given an AIServiceValue.
     * @return an equivalent ServiceValue object.
     */
    public static ServiceValue convertAIServiceToService(AIServiceValue aiservice,
                                                         ServiceManagerLocal serviceMgr) 
        throws FinderException {

        ServiceTypeValue stValue =
            serviceMgr.findServiceTypeByName(aiservice.getServiceTypeName());

        ServiceValue service = new ServiceValue();
        service.setDescription(aiservice.getDescription());
        service.setName(aiservice.getName());
        service.setServiceType(stValue);
        service.setAutodiscoveryZombie(false);
        return service;
    }

    public static void sendCreateEvent(AuthzSubjectValue subject,
                                       AppdefEntityID aid) {
        ResourceCreatedZevent zevent =
                    new ResourceCreatedZevent(subject, aid);
        ZeventManager.getInstance().enqueueEventAfterCommit(zevent);
    }

    public static void sendNewConfigEvent(AuthzSubjectValue subject,
                                          AppdefEntityID aid) {
        AppdefEvent event = new AppdefEvent(subject, aid,
                                            AppdefEvent.ACTION_NEWCONFIG);
        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }

    public static void sendNewConfigEvent(AuthzSubjectValue subject,
                                          AppdefEntityID aid,
                                          AllConfigResponses config) {
        AppdefEvent event = new AppdefEvent(subject, aid,
                                            AppdefEvent.ACTION_NEWCONFIG,
                                            config);

        Messenger sender = new Messenger();
        sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }
}
