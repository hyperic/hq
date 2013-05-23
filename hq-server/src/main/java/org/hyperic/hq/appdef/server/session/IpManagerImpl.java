/*                                                                 
 * NOTE: This copyright does *not* cover user programs that use HQ 
 * program services by normal system calls through the application 
 * program interfaces provided as part of the Hyperic Plug-in Development 
 * Kit or the Hyperic Client Development Kit - this is merely considered 
 * normal use of the program, and does *not* fall under the heading of 
 * "derived work". 
 *  
 * Copyright (C) [2004-2013], VMware, Inc. 
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
package org.hyperic.hq.appdef.server.session;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.IpManager;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.util.Classifier;
import org.hyperic.util.Transformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class IpManagerImpl implements IpManager {
    
    @Autowired
    private IpDAO ipDAO;
    
    public Map<Resource, Collection<Ip>> getIps(Collection<Resource> platformResources) {
        Collection<Integer> platformIds = new Transformer<Resource, Integer>() {
            @Override
            public Integer transform(Resource r) {
                Integer resourceTypeId = r.getResourceType().getId();
                if (resourceTypeId.equals(AuthzConstants.authzPlatform)) {
                    return r.getInstanceId();
                }
                return null;
            }
        }.transform(platformResources);
        final List<Ip> ips = ipDAO.getIps(platformIds);
        return new Classifier<Ip, Resource, Ip>() {
            @Override
            public NameValue<Resource, Ip> classify(Ip ip) {
                return new NameValue<Resource, Ip>(ip.getPlatform().getResource(), ip);
            }
        }.classify(ips);
    }

}
