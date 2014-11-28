/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import com.vmware.hyperic.model.relations.CommonModel;
import com.vmware.hyperic.model.relations.Identifier;
import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.Relation;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceTier;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author laullon
 */
public class DiscoveryVRAServer extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVRAServer.class);

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        log.debug("[getServerResources] platformConfig=" + platformConfig);

        List<ServerResource> servers = super.getServerResources(platformConfig);

        Properties props = VRAUtils.configFile();
        String cspHost = props.getProperty("csp.host");
        String websso = props.getProperty("vmidentity.websso.host");

        log.debug("[getServerResources] csp.host=" + cspHost);
        log.debug("[getServerResources] csp.host=" + websso);

        if (cspHost != null) {
            for (ServerResource server : servers) {
                String model = marshallCommonModel(getCommonModel(cspHost, websso));
                server.getProductConfig().setValue("extended-relationship-model", new String(Base64.encodeBase64(model.getBytes())));

                ConfigResponse c = new ConfigResponse();
                c.setValue("extended-relationship-model", new String(Base64.encodeBase64(model.getBytes())));

                // do not remove, why? please don't ask.
                server.setProductConfig(server.getProductConfig());
                server.setCustomProperties(c);
            }
        }
        return servers;
    }

    public String marshallCommonModel(CommonModel model) {
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        model.marshallAsXml(fos);
        log.debug("[testMarshallAsJson] fos=" + fos.toString());
        return fos.toString();
    }

    private CommonModel getCommonModel(String lbHostNAme, String websso) {
        ObjectFactory factory = new ObjectFactory();

        // 1. Create the Load Balancer
        Identifier lbIdentifier = factory.createIdentifier();
        lbIdentifier.setName("hostname");
        lbIdentifier.setValue(lbHostNAme);

        Resource lbResource = factory.createResource();
        lbResource.setCreateIfNotExist(Boolean.TRUE);
        lbResource.setName("vRA #1 Load Balancer");
        lbResource.setType("vRealize Automation Load Balancer");
        lbResource.setTier(ResourceTier.SERVER);
        lbResource.getIdentifiers().add(lbIdentifier);

        Relation lbRelation = factory.createRelation();
        lbRelation.setType(RelationType.PARENT);
        lbRelation.setCreateIfNotExist(Boolean.TRUE);
        lbRelation.setResource(lbResource);

        // 1.1 Create the Load Balancers Group (Tag)
        final String lbTagName = "vRealize Automation Load Balancers";
        Identifier lbTagIdentifier = factory.createIdentifier();
        lbTagIdentifier.setName("tag.name");
        lbTagIdentifier.setValue(lbTagName);

        Resource lbTagResource = factory.createResource();
        lbTagResource.setCreateIfNotExist(Boolean.TRUE);
        lbTagResource.setName(lbTagName);
        lbTagResource.setType(lbTagName);
        lbTagResource.setTier(ResourceTier.LOGICAL);
        lbTagResource.getIdentifiers().add(lbTagIdentifier);

        Relation lbTagRelation = factory.createRelation();
        lbTagRelation.setType(RelationType.PARENT);
        lbTagRelation.setCreateIfNotExist(Boolean.TRUE);
        lbTagRelation.setResource(lbTagResource);

        lbResource.getRelations().add(lbTagRelation);

        // 1.1.1 Create the Load Balancers Group (Tag)
        final String lbsTagName = "Load Balancers";
        Identifier lbsTagIdentifier = factory.createIdentifier();
        lbsTagIdentifier.setName("tag.name");
        lbsTagIdentifier.setValue(lbsTagName);

        Resource lbsTagResource = factory.createResource();
        lbsTagResource.setCreateIfNotExist(Boolean.TRUE);
        lbsTagResource.setName(lbsTagName);
        lbsTagResource.setType(lbsTagName);
        lbsTagResource.setTier(ResourceTier.LOGICAL);
        lbsTagResource.getIdentifiers().add(lbsTagIdentifier);

        Relation lbsTagRelation = factory.createRelation();
        lbsTagRelation.setType(RelationType.PARENT);
        lbsTagRelation.setCreateIfNotExist(Boolean.TRUE);
        lbsTagRelation.setResource(lbsTagResource);

        lbTagResource.getRelations().add(lbsTagRelation);

        // 2. Create the SSO
        Identifier ssoIdentifier = factory.createIdentifier();
        ssoIdentifier.setName("hostname");
        ssoIdentifier.setValue(websso);

        Resource ssoResource = factory.createResource();
        ssoResource.setCreateIfNotExist(Boolean.TRUE);
        ssoResource.setName(websso);
        ssoResource.setType("vRA SSO Server");
        ssoResource.setTier(ResourceTier.SERVER);
        ssoResource.getIdentifiers().add(ssoIdentifier);

        Relation ssoRelation = factory.createRelation();
        ssoRelation.setType(RelationType.SIBLING);
        ssoRelation.setCreateIfNotExist(Boolean.TRUE);
        ssoRelation.setResource(ssoResource);

        // 2.1 Create the SSO Tag
        final String ssoTagName = "SSO";
        Identifier ssoTagIdentifier = factory.createIdentifier();
        ssoTagIdentifier.setName("tag.name");
        ssoTagIdentifier.setValue(ssoTagName);

        Resource ssoTagResource = factory.createResource();
        ssoTagResource.setCreateIfNotExist(Boolean.TRUE);
        ssoTagResource.setName(ssoTagName);
        ssoTagResource.setType(ssoTagName);
        ssoTagResource.setTier(ResourceTier.LOGICAL);
        ssoTagResource.getIdentifiers().add(ssoTagIdentifier);

        Relation ssoTagRelation = factory.createRelation();
        ssoTagRelation.setType(RelationType.PARENT);
        ssoTagRelation.setCreateIfNotExist(Boolean.TRUE);
        ssoTagRelation.setResource(ssoTagResource);

        ssoResource.getRelations().add(ssoTagRelation);

        // 3. 
        // 4. Create the generic model
        CommonModel model = factory.createRelationshipModel(null);
        model.getRelations().add(lbRelation);
        model.getRelations().add(ssoRelation);

        return model;
    }
}
