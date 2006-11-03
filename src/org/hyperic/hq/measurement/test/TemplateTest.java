package org.hyperic.hq.measurement.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.MeasurementTemplateDAO;
import org.hyperic.hq.dao.MonitorableTypeDAO;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementTemplate;
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
public class TemplateTest 
    extends HQEJBTestBase
{
    private Log _log = LogFactory.getLog(TemplateTest.class.getName());

    /**
     * For our tests we use the squid plugin, would be good to create
     * our own example plugin for testing in the future
     */
    private static final String _plugin = "squid";
    private TypeInfo[] _types;
    private Map _m = new HashMap(); // Map of TypeInfo->MeasurementInfo[]

    public TemplateTest(String string) {
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

    private MeasurementTemplateDAO getMeasurementTemplateDAO() {
        return DAOFactory.getDAOFactory().getMeasurementTemplateDAO();
    }

    private MonitorableTypeDAO getMonitorableTypeDAO() {
        return DAOFactory.getDAOFactory().getMonitorableTypeDAO();
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
    protected void createTemplates() throws Exception {

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
            MeasurementTemplateValue val = manager.getTemplate(ids[0]);
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

    protected void cleanup() throws Exception {

        TemplateManagerLocal manager = getTemplateManager();
        AuthzSubjectValue subject = getOverlord();

        for (int i = 0; i < _types.length; i++) {
            Integer id = manager.getMonitorableTypeId(_plugin, _types[i]);
            List tmpls = getMeasurementTemplateDAO().
                findRawByMonitorableType(id);

            for (int j = 0; j < tmpls.size(); j++) {
                MeasurementTemplate mt =
                    (MeasurementTemplate)tmpls.get(j);
                manager.removeTemplate(subject, mt.getId());
            }
        }
    }

    public void testSimple() throws Exception {

        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    cleanup();
                }
            });

        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    registerMonitorableTypes();
                }
            });

        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    createTemplates();
                }
            });

        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    findTemplates();
                }
            });

        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    updateTemplates();
                }
            });

        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    cleanup();
                }
            });
    }
}
