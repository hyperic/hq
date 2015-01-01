/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import static org.hyperic.plugin.vrealize.automation.VRAUtils.createLogialResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFqdn;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.marshallResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.setModelProperty;
import static org.hyperic.plugin.vrealize.automation.VraConstants.CREATE_IF_NOT_EXIST;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_APPLICATION;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_DATABASES_GROUP;
import static org.hyperic.plugin.vrealize.automation.VraConstants.TYPE_VRA_MANAGER_SERVER_TAG;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceTier;

/**
 *
 * @author glaullon, imakhlin
 */
public class DiscoveryVRAManagerServer extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAManagerServer.class);

    @Override
    protected ServerResource newServerResource(long pid,
                                               String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = new File(exe + ".config");
        log.debug("[newServerResource] configFile=" + configFile);

        String vraApplicationEndPointFqdn = executeXMLQuery("//serviceConfiguration/@authorizationStore", configFile);
        if (!StringUtils.isEmpty(vraApplicationEndPointFqdn)) {
            vraApplicationEndPointFqdn = VRAUtils.getFqdn(vraApplicationEndPointFqdn);
        }
        log.debug("[newServerResource] vraApplicationEndPointFqdn (authorizationStore) = '"
                    + vraApplicationEndPointFqdn + "'");

        String bdconnInfo = executeXMLQuery("//serviceConfiguration/@connectionString", configFile);
        log.debug("[newServerResource] bdConn (connectionString) = '" + bdconnInfo + "'");
        String vraManagerDatabaseFqdn = null;
        if (!StringUtils.isEmpty(bdconnInfo)) {
            String p = "Data Source=";
            int i = bdconnInfo.indexOf(p) + p.length();
            int f = bdconnInfo.indexOf(";", i);
            if ((i > -1) && (f > -1)) {
                vraManagerDatabaseFqdn = bdconnInfo.substring(i, f).trim();
            }
        }
        log.debug("[newServerResource] vraManagerDatabaseFqdn (Data Source) = '" + vraManagerDatabaseFqdn + "'");

        Resource modelResource = getCommonModel(server, vraApplicationEndPointFqdn, vraManagerDatabaseFqdn);
        String modelXml = marshallResource(modelResource);

        log.info("[newServerResource] modelXml \n\n" + modelXml + "\n\n");
        setModelProperty(server, modelXml);

        return server;
    }

    /**
     * @param server
     * @param vRALB
     * @param dbFQDN
     * @return
     */
    private Resource getCommonModel(ServerResource server,
                                    String vraApplicationEndPointFqdn,
                                    String vraManagerDatabaseServerFqdn) {

        ObjectFactory factory = new ObjectFactory();

        Resource vraApplication =
                    createLogialResource(
                                factory, TYPE_VRA_APPLICATION, vraApplicationEndPointFqdn);
        Resource vraManagerServersGroup =
                    createLogialResource(factory, TYPE_VRA_MANAGER_SERVER_TAG, vraApplicationEndPointFqdn);

        Resource vraManagerServer = factory.createResource(!CREATE_IF_NOT_EXIST, server.getType(),
                    server.getName(), ResourceTier.SERVER);

        Resource vraDatabasesGroup =
                    createLogialResource(factory, TYPE_VRA_DATABASES_GROUP, vraApplicationEndPointFqdn);

        vraDatabasesGroup.addRelations(factory.createRelation(vraApplication, RelationType.PARENT));

        Resource databaseServerHost =
                    factory.createResource(!CREATE_IF_NOT_EXIST, VraConstants.TYPE_WINDOWS,
                                vraManagerDatabaseServerFqdn, ResourceTier.PLATFORM);

        databaseServerHost.addRelations(
                    factory.createRelation(vraDatabasesGroup, RelationType.PARENT));

        vraManagerServer.addRelations(factory.createRelation(vraManagerServersGroup, RelationType.PARENT),
                    factory.createRelation(databaseServerHost, RelationType.CHILD));

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
