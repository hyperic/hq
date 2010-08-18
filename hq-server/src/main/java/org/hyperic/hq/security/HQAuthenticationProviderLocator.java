/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.security;


import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import java.util.Collections;

/**
 * Performs classpath scanning to lookup all implementations of
 * {@link HQAuthenticationProvider} for use by the @{link
 * {@link InternalAuthenticationProvider}
 * @author jhickey
 * 
 */
public class HQAuthenticationProviderLocator implements FactoryBean<List<HQAuthenticationProvider>>,
    ApplicationContextAware {
    private ApplicationContext applicationContext;

    public List<HQAuthenticationProvider> getObject() throws Exception {
        Map<String,HQAuthenticationProvider> providers= applicationContext.getBeansOfType(
            HQAuthenticationProvider.class);
        List<HQAuthenticationProvider> orderedProviders = new ArrayList<HQAuthenticationProvider>(providers.values());
        Collections.sort(orderedProviders, new OrderComparator());
        return orderedProviders;
    }

    public Class<?> getObjectType() {
        return List.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
