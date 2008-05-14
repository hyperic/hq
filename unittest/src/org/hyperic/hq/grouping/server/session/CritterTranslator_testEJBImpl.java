/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.grouping.server.session;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.MixedGroupType;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.grouping.Critter;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterTranslator;
import org.hyperic.hq.grouping.critters.AvailabilityCritterType;
import org.hyperic.hq.grouping.critters.CompatGroupTypeCritterType;
import org.hyperic.hq.grouping.critters.MixedGroupTypeCritterType;
import org.hyperic.hq.grouping.critters.OwnedCritterType;
import org.hyperic.hq.grouping.critters.ProtoNameCritterType;
import org.hyperic.hq.grouping.critters.ResourceNameCritterType;
import org.hyperic.hq.grouping.critters.ResourceTypeCritterType;
import org.hyperic.hq.grouping.shared.CritterTranslator_testLocal;
import org.hyperic.hq.grouping.shared.CritterTranslator_testUtil;
import org.hyperic.hq.measurement.shared.AvailabilityType;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * The session bean implementing the in-container unit tests for the 
 * AuthzSubjectManager.
 * 
 * @ejb:bean name="CritterTranslator_test"
 *      jndi-name="ejb/authz/CritterTranslator_test"
 *      local-jndi-name="LocalCritterTranslator_test"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:util generate="physical"
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class CritterTranslator_testEJBImpl implements SessionBean {
    
    Log _log = LogFactory.getLog(CritterTranslator_testEJBImpl.class);

    public static CritterTranslator_testLocal getOne() {
        try {
            return CritterTranslator_testUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * @ejb:interface-method
     */
    public void testTranslate() throws Exception {
        testResourceType(AuthzConstants.platformResType, 6);
        testResourceType(AuthzConstants.serverResType, 64);
        testResourceType(AuthzConstants.serviceResType, 658);
        testResourceType(AuthzConstants.groupResourceTypeName, 9);
        testCompatCritter("Linux", 2);
        testResourceNameCritter("HQ", 181);
        testMixedCritter();
        testAvailabilityCritter();
        // currently this mechanism seems broken as admin owns most of the
        // resources in EAM_RESOURCE, not hqadmin
        // see http://jira.hyperic.com/browse/HHQ-2094
//        testOwnershipCritter("hqadmin", 104);
        testComplexCritter();
    }

    private void testOwnershipCritter(String subject, int expectedSize) {
        OwnedCritterType type = new OwnedCritterType();
        AuthzSubjectManagerLocal aMan = AuthzSubjectManagerEJBImpl.getOne();
        AuthzSubject subj = aMan.findSubjectByName(subject);
        testCritter(subject, type.newInstance(subj), expectedSize);
    }

    private void testComplexCritter() {
        List critters = new ArrayList();
        ProtoNameCritterType rtType = new ProtoNameCritterType();
        critters.add(rtType.newInstance("Sendmail 8.x"));
        AvailabilityCritterType atype = new AvailabilityCritterType();
        critters.add(atype.newInstance(AvailabilityType.AVAIL_DOWN));
        ResourceNameCritterType type = new ResourceNameCritterType();
        critters.add(type.newInstance("sendmail|hq"));
        CritterList cList = new CritterList(critters, false);
        testCritter("Sendmail 8.x Proto/availDown/sendmail|hq ALL", cList, 5);

        critters.clear();
        critters.add(atype.newInstance(AvailabilityType.AVAIL_UP));
        critters.add(type.newInstance("sendmail|hq"));
        cList = new CritterList(critters, true);
        testCritter("Sendmail 8.x Proto/availUp/sendmail|hq ANY", cList, 194);
    }

    private void testAvailabilityCritter()
    {
        AvailabilityType avail = AvailabilityType.AVAIL_UP;
        AvailabilityCritterType type = new AvailabilityCritterType();
        testCritter(avail, type.newInstance(avail), 700);
        avail = AvailabilityType.AVAIL_DOWN;
        testCritter(avail, type.newInstance(avail), 5);
    }

    private void testResourceNameCritter(String pattern, int expectedSize) {
        ResourceNameCritterType type = new ResourceNameCritterType();
        testCritter(pattern, type.newInstance(pattern), expectedSize);
    }

    private void testMixedCritter() {
        Integer[] groupTypes =
            (Integer[])MixedGroupType.ALL_MIXED_GROUPS
                .getAppdefEntityTypes().toArray(new Integer[0]);
        MixedGroupType type = MixedGroupType.findByCode(groupTypes);
        MixedGroupTypeCritterType critter =
            new MixedGroupTypeCritterType();
        ResourceTypeCritterType rtype = new ResourceTypeCritterType();
        List critters = new ArrayList();
        critters.add(rtype.newInstance(AuthzConstants.groupResourceTypeName));
        critters.add(critter.newInstance(type));
        CritterList cList = new CritterList(critters, false);
        testCritter("mixed groups", cList, 3);
    }

    private void testCompatCritter(String ProtoName, int expectedSize) {
        ResourceManagerLocal rman = ResourceManagerEJBImpl.getOne();
        Resource proto = rman.findResourcePrototypeByName(ProtoName);
        CompatGroupTypeCritterType type = new CompatGroupTypeCritterType();
        testCritter(ProtoName, type.newInstance(proto), expectedSize);
    }
    
    private void testCritter(Object prop, Critter critter, int expectedSize) {
        List critters = new ArrayList();
        critters.add(critter);
        CritterList cList = new CritterList(critters, false);
        testCritter(prop, cList, expectedSize);
    }

    private void testCritter(Object prop, CritterList cList, int expectedSize) {
        CritterTranslator ct = new CritterTranslator();
        CritterTranslationContext ctx = getTranslationContext();
        PageList list = ct.translate(ctx, cList, PageControl.PAGE_ALL);
        _log.info("prop description -> " + prop +
                  ", list.getTotalSize() -> " + list.getTotalSize() +
                  " vs. expected -> " + expectedSize);
        Assert.assertTrue(list.getTotalSize() == expectedSize);
    }

    private CritterTranslationContext getTranslationContext() {
        AuthzSubjectManagerLocal aMan = AuthzSubjectManagerEJBImpl.getOne();
        AuthzSubject overlord = aMan.getOverlordPojo();
        return new CritterTranslationContext(overlord);
    }

    private void testResourceType(String resourceType, int expectedSize)
        throws Exception {
        ResourceTypeCritterType type = new ResourceTypeCritterType();
        testCritter(resourceType, type.newInstance(resourceType), expectedSize);
    }
    
    public void ejbCreate() throws CreateException {}
    public void ejbActivate() throws EJBException, RemoteException {}
    public void ejbPassivate() throws EJBException, RemoteException {}
    public void ejbRemove() throws EJBException, RemoteException {}
    public void setSessionContext(SessionContext arg0) {}
}
