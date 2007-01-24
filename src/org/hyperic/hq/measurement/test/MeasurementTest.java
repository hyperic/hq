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

package org.hyperic.hq.measurement.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.TemplateManagerLocal;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.test.HQEJBTestBase;
import org.hyperic.util.pager.PageControl;

/**
 * Unit tests for Measurement templates.  These tests use seed data from the
 * example plugins found in plugins/examples.
 */
public class MeasurementTest
    extends HQEJBTestBase
{
    private Log _log = LogFactory.getLog(MeasurementTest.class.getName());

    /**
     * For our tests we use the netscaler plugin, would be good to create
     * our own example plugin for testing in the future
     */
    private static final String _plugin = "netscaler";
    private TypeInfo[] _types;
    private Map _m = new HashMap(); // Map of TypeInfo->MeasurementInfo[]

    public MeasurementTest(String string) {
        super(string);
    }

    /**
     * Load the TypeInfo's and MeasurementInfo's for our tests.
     */
    public void setUp() 
        throws Exception
    {
        super.setUp();

        String pluginDir = System.getProperty("hq.example.plugins");
        _log.info("Loading example plugins from " + pluginDir);
        ProductPluginManager ppm = 
            new ProductPluginManager(System.getProperties());
        ppm.setRegisterTypes(true); // Required for MeasurementInfo's
        ppm.init();
        int num = ppm.registerPlugins(pluginDir);
        _log.info("Loaded " + num + " plugins");

        ProductPlugin plugin = ppm.getProductPlugin(_plugin);
        _types = plugin.getTypes();
        for (int i = 0; i < _types.length; i++) {
            MeasurementInfo[] m;
            try {
                m = ppm.getMeasurementPluginManager().
                    getMeasurements(_types[i]);
                _m.put(_types[i].getName(), m);
            } catch (PluginNotFoundException e) {
                _log.info(_types[i].getName() +
                          " does not support measurement");
                return;
            }
        }
        ppm.shutdown();
    }

    /**
     * Test registering MonitorableTypes
     */
    private void registerMonitorableTypes() throws Exception {

        TemplateManagerLocal manager = getTemplateManager();

        for (int i = 0; i < _types.length; i++) {
            Integer id = manager.getMonitorableTypeId(_plugin, _types[i]);
        }
    }

    /**
     * Test metric template insertion
     */
    private void createTemplates() throws Exception {

        TemplateManagerLocal manager = getTemplateManager();

        Map types = new HashMap();
        for (int i = 0; i < _types.length; i++) {
            Integer id = manager.getMonitorableTypeId(_plugin, _types[i]);
            assertNotNull(id);

            Map templates = new HashMap();
            MeasurementInfo m[] =
                (MeasurementInfo[])_m.get(_types[i].getName());
            if (m != null) {
                for (int j = 0; j < m.length; j++) {
                    templates.put(m[j].getAlias(), m[j]);
                }
                types.put(id, templates);
            }
        }
        manager.createTemplates(_plugin, types);
    }

    /**
     * Test the various finder methods for MeasurementTemplate
     */
    private void findTemplates() throws Exception {

        TemplateManagerLocal manager = getTemplateManager();

        for (int i = 0; i < _types.length; i++) {
            TypeInfo info = _types[i];
            MeasurementInfo[] m = (MeasurementInfo[])_m.get(info.getName());

            // Test find template ID's by MonitorableType name
            _log.info("Testing find template ids");
            Integer ids[] = manager.findTemplateIds(info.getName());
            assertEquals(m.length, ids.length);

            // Test single lookup
            _log.info("Testing single lookup");
            MeasurementTemplateValue val = manager.getTemplateValue(ids[0]);
            assertNotNull(val);
            assertEquals(ids[0], val.getId());

            List tmpls;
            // Test find by array of ids
            _log.info("Testing find by template ids");
            tmpls = manager.getTemplates(ids, PageControl.PAGE_ALL);
            assertEquals(ids.length, tmpls.size());

            // Test find by MonitorableType
            _log.info("Testing find by monitorable type");
            tmpls = manager.findTemplates(info.getName(), null, null,
                                          new PageControl());
            assertEquals(ids.length, tmpls.size());

            // Test find by MonitorableType and Category
            _log.info("Testing find by monitorable type and category");
            tmpls =
                manager.findTemplates(info.getName(),
                                      MeasurementConstants.CAT_AVAILABILITY,
                                      null, new PageControl());
            int numAvail = 0;  // Count avail from MeasurementInfo's
            for (int j = 0; j < m.length; j++) {
                if (m[j].getCategory().
                    equals(MeasurementConstants.CAT_AVAILABILITY)) {
                    numAvail++;
                }
            }
            assertEquals(numAvail, tmpls.size());
            
            // Test find using filters, using AVAILABILTY
            _log.info("Testing find using filters");
            long filter =
                MeasurementConstants.FILTER_AVAIL |
                MeasurementConstants.FILTER_DYN |
                MeasurementConstants.FILTER_TREND_UP |
                MeasurementConstants.FILTER_TREND_DN |
                MeasurementConstants.FILTER_STATIC;
            tmpls = manager.findTemplates(info.getName(), filter, null);
            assertEquals(numAvail, tmpls.size());
            
            // Test find using keywords
            String name = m[0].getName();
            _log.info("Testing find using keywords");
            tmpls = manager.findTemplates(info.getName(),
                                          MeasurementConstants.FILTER_NONE,
                                          name);
            assertEquals(1, tmpls.size());

            // Test find default metrics
            _log.info("Testing find default metrics");
            tmpls = manager.findDefaultTemplates(info.getName(),
                                                 info.getType());
            assertTrue(tmpls.size() > 0);

            // Test find designated metrics
            _log.info("Testing find designated metrics");
            tmpls = manager.findDesignatedTemplates(info.getName(),
                                                    info.getType());
            assertTrue(tmpls.size() > 0);
        }
    }

    private void updateTemplates () throws Exception {
        TemplateManagerLocal manager = getTemplateManager();

        for (int i = 0; i < _types.length; i++) {
            TypeInfo info = _types[i];
            MeasurementInfo[] m = (MeasurementInfo[])_m.get(info.getName());
            Integer id = manager.getMonitorableTypeId(_plugin, info);

            _log.info("Testing template update");
            long interval = 12345;
            for (int j = 0; j < m.length; j++) {
                m[j].setInterval(interval); // Set default intervals to 12345
            }
            manager.updateTemplates(_plugin, info, id, m);
        }
    }

    private void cleanup() throws Exception {

        TemplateManagerLocal manager = getTemplateManager();
        AuthzSubjectValue subject = getOverlord();

        for (int i = 0; i < _types.length; i++) {
            Integer ids[] = manager.findTemplateIds(_types[i].getName());
            _log.info("Found " + ids.length + " templates for " + _types[i]);       
            for (int j = 0; j < ids.length; j++) {
                manager.removeTemplate(subject, ids[j]);
            }
        }
    }

    public void testSimple() throws Exception {
        // Make sure the test environment is clean
        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    cleanup();
                }
            });
        // Register the monitorable types
        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    registerMonitorableTypes();
                }
            });
        // Create the measurement templates for the monitorable types
        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    createTemplates();
                }
            });
        // Run tests on MeasurementTemplate finders
        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    findTemplates();
                }
            });
        // Test update of Templates
        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    updateTemplates();
                }
            });
        // Clean up for next time
        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    cleanup();
                }
            });
    }
}
