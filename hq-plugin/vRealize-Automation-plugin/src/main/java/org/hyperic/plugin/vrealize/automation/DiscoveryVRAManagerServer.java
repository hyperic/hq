/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static org.hyperic.plugin.vrealize.automation.VRAUtils.*;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.KEY_APPLICATION_NAME;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_DATABASES_GROUP;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_MANAGER_SERVER_TAG;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 * @author glaullon, imakhlin
 */
public class DiscoveryVRAManagerServer extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAManagerServer.class);

//    @Override
//    public List<ServerResource> getServerResources(ConfigResponse platformConfig)
//        throws PluginException {
//        log.debug("[getServerResources] platformConfig=" + platformConfig);
//        String platformFqdn = platformConfig.getValue("platform.fqdn");
//        VRAUtils.setLocalFqdn(platformFqdn);
//        log.debug("[getServerResources] platformFqdn=" + platformFqdn);
//
//        @SuppressWarnings("unchecked")
//        List<ServerResource> servers = super.getServerResources(platformConfig);
//
//        return servers;
//    }

    @Override
    protected ServerResource newServerResource(
                long pid, String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = new File(exe + ".config");
        log.debug("[newServerResource] configFile=" + configFile);

        String vraApplicationEndPointFqdn = executeXMLQuery("//serviceConfiguration/@authorizationStore", configFile);
        if (StringUtils.isNotBlank(vraApplicationEndPointFqdn)) {
            vraApplicationEndPointFqdn = VRAUtils.getFqdn(vraApplicationEndPointFqdn);
        }
        log.debug("[newServerResource] vraApplicationEndPointFqdn (authorizationStore) = '" + vraApplicationEndPointFqdn
                    + "'");

        String bdconnInfo = executeXMLQuery("//serviceConfiguration/@connectionString", configFile);
        log.debug("[newServerResource] bdConn (connectionString) = '" + bdconnInfo + "'");

        AddressExtractor addressExtractor = createAddressExtractor();

        String vraManagerDatabaseFqdn = VRAUtils.getFqdn(bdconnInfo, addressExtractor);

        log.debug("[newServerResource] vraManagerDatabaseFqdn (Data Source) = '" + vraManagerDatabaseFqdn + "'");

        Resource modelResource = getCommonModel(server, vraApplicationEndPointFqdn, vraManagerDatabaseFqdn);
        String modelXml = marshallResource(modelResource);

        setModelProperty(server, modelXml);

        return server;
    }

    private AddressExtractor createAddressExtractor() {
        AddressExtractor addressExtractor = new AddressExtractor() {

            public String extractAddress(String containsAddress) {
                String vraManagerDatabaseFqdn = null;
                if (!StringUtils.isEmpty(containsAddress)) {
                    String p = "Data Source=";
                    int i = containsAddress.indexOf(p) + p.length();
                    int f = containsAddress.indexOf(";", i);
                    if ((i > -1) && (f > -1)) {
                        vraManagerDatabaseFqdn = containsAddress.substring(i, f).trim();
                    }
                    return vraManagerDatabaseFqdn;
                }
                return vraManagerDatabaseFqdn;
            }
        };
        return addressExtractor;
    }

    private Resource getCommonModel(
                ServerResource server, String vraApplicationEndPointFqdn, String vraManagerDatabaseServerFqdn) {

        ObjectFactory factory = new ObjectFactory();

        Resource vraApplication = factory.createApplicationResource(TYPE_VRA_APPLICATION, vraApplicationEndPointFqdn);
        vraApplication.addProperty(factory.createProperty(KEY_APPLICATION_NAME, vraApplicationEndPointFqdn));

        Resource vraManagerServersGroup =
                    factory.createLogicalResource(TYPE_VRA_MANAGER_SERVER_TAG, vraApplicationEndPointFqdn);

        Resource vraManagerServer = factory.createResource(!CREATE_IF_NOT_EXIST, server.getType(), server.getName(),
                    ResourceTier.SERVER);

        Resource vraDatabasesGroup =
                    factory.createLogicalResource(TYPE_VRA_DATABASES_GROUP, vraApplicationEndPointFqdn);

        vraDatabasesGroup.addRelations(factory.createRelation(vraApplication, RelationType.PARENT));

        // If database server resides on this machine then skip it to avoid cyclic reference
        Resource databaseServerHost = factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_WINDOWS,
                    vraManagerDatabaseServerFqdn, ResourceTier.PLATFORM);
        databaseServerHost.addRelations(factory.createRelation(vraDatabasesGroup, RelationType.PARENT));

        InetAddress addr = null;
        String hostname = null;
        try {
            addr = InetAddress.getLocalHost();
            hostname = addr.getCanonicalHostName();
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
            hostname = getFqdn("localhost");
            log.debug(String.format("[getCommonModel] hostname is: '%s'", hostname));
        }

        if (VRAUtils.areFqdnsEquivalent(hostname, vraManagerDatabaseServerFqdn)) {
            vraManagerServer.addRelations(factory.createRelation(databaseServerHost,
                        VRAUtils.getDataBaseRalationType(vraManagerDatabaseServerFqdn)));
        }

        vraManagerServer.addRelations(factory.createRelation(vraManagerServersGroup, RelationType.PARENT));

        vraManagerServersGroup.addRelations(factory.createRelation(vraApplication, RelationType.PARENT));

        return vraManagerServer;
    }

    /* inline unit test
    @Test
    public void test() {
        ServerResource server = new ServerResource();
        server.setName("111");
        server.setType("222");
        Resource modelResource = getCommonModel(server, "AAA", "BBB");
        String modelXml = marshallResource(modelResource);
        Assert.assertNotNull(modelXml);

        System.out.println(modelXml);
    }
    */
}
