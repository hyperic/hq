/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import com.vmware.hyperic.model.relations.Identifier;
import com.vmware.hyperic.model.relations.ObjectFactory;
import static com.vmware.hyperic.model.relations.RelationType.PARENT;
import static com.vmware.hyperic.model.relations.RelationType.SIBLING;
import com.vmware.hyperic.model.relations.Resource;
import com.vmware.hyperic.model.relations.ResourceSubType;
import static com.vmware.hyperic.model.relations.ResourceTier.LOGICAL;
import static com.vmware.hyperic.model.relations.ResourceTier.SERVER;
import static org.hyperic.plugin.vrealize.automation.VraConstants.*;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;
import org.w3c.dom.Document;

/**
 *
 * @author glaullon
 */
public class DiscoveryVRAIaasWeb extends DaemonDetector {

    private static final Log log = LogFactory.getLog(DiscoveryVRAServer.class);

    @Override
    protected ServerResource newServerResource(long pid, String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = null;
        try {
            String[] args = getSigar().getProcArgs(pid);
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equalsIgnoreCase("-h")) {
                    configFile = new File(args[i + 1]);
                }
            }
        } catch (SigarException ex) {
            log.debug(ex, ex);
        }

        log.debug("[newServerResource] configFile=" + configFile);
        if (configFile != null) {
            String mmwebPath = executeXMLQuery("//application[@applicationPool='RepositoryAppPool']/virtualDirectory[@path='/']/@physicalPath", configFile);
            if (mmwebPath != null) {
                File installPath = new File(mmwebPath, "../..");
                log.debug("[newServerResource] installPath=" + getCanonicalPath(installPath));
                server.setInstallPath(getCanonicalPath(installPath));
                server.setIdentifier(server.getIdentifier().replace("%installpath%", server.getInstallPath()));

                File dataCfg = new File(installPath, "Server/Model Manager Data/Repoutil.exe.config");
                log.debug("[newServerResource] dataCfg=" + getCanonicalPath(dataCfg));
                String vRAIaaSWebLoadBalancer = executeXMLQuery("//add[@key='repositoryAddress']/@value", dataCfg);
                vRAIaaSWebLoadBalancer = vRAIaaSWebLoadBalancer.replaceAll("\\w*://([^:/]*).*", "$1");
                log.debug("[newServerResource] vRAIaaSWebLoadBalancer=" + vRAIaaSWebLoadBalancer);

                File webCfg = new File(installPath, "Server/Model Manager Web/Web.config");
                log.debug("[newServerResource] webCfg=" + getCanonicalPath(webCfg));
                String vRAServer = executeXMLQuery("//repository/@store", webCfg);
                vRAServer = vRAServer.replaceAll("\\w*://([^:/]*).*", "$1");
                log.debug("[newServerResource] vRAServer=" + vRAServer);

                String model = VRAUtils.marshallResource(getIaaSWebServerRelationsModel(vRAServer, vRAIaaSWebLoadBalancer));
                ConfigResponse c = new ConfigResponse();
                c.setValue(PROP_EXTENDED_REL_MODEL, new String(Base64.encodeBase64(model.getBytes())));

                // do not remove, why? please don't ask.
                server.setProductConfig(server.getProductConfig());
                server.setCustomProperties(c);

            }
        }
        return server;
    }

    public static Resource getIaaSWebServerRelationsModel(String vRaApplicationName, String vRAIaaSWebLoadBalancer) {
        ObjectFactory factory = new ObjectFactory();

        final String iaasWebServerResourceType = "vRealize Automation IaaS Web";
        final String iaasWebServerName = "iaas-web-server-1.local";
        Resource iaasWebServer = factory.createResource(!CREATE_IF_NOT_EXIST, iaasWebServerResourceType,
                iaasWebServerName, SERVER);

        final String iassWebServerTagType = "vRealize Automation IaaS Web";
        Resource iaasWebServerTag = createLogialResource(factory, vRaApplicationName, iassWebServerTagType);;
        iaasWebServer.addRelations(factory.createRelation(iaasWebServerTag, PARENT));

        final String vraAppTagType = "vRealize Automation Application";
        Resource vraAppTagResource = getVraAppTag(factory, vRaApplicationName, vraAppTagType);
        iaasWebServerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        //This is only if there is a load balancer. If not then nothing load balancer related should be created.
        final String lbResourceType = "vRealize Automation IaaS Web Load Balancer Service";
        final String lbResourceName = "lg-web.ra.local";
        Resource loadBalancer = factory.createResource(Boolean.FALSE, lbResourceType, lbResourceName, SERVER);;
        iaasWebServer.addRelations(factory.createRelation(loadBalancer, SIBLING));

        final String lbTagType = "vRealize Automation Load Balancer";
        Resource loadBalancerTag = createLogialResource(factory, vRaApplicationName, lbTagType);
        loadBalancer.addRelations(factory.createRelation(loadBalancerTag, PARENT));
        loadBalancerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        final String lbsTagType = "Load Balancer";
        Resource loadBalancerSuperTag = createLogialResource(factory, lbTagType, lbsTagType);
        loadBalancerTag.addRelations(factory.createRelation(loadBalancerSuperTag, PARENT));

        final String vcoName = "ra-vco-a2-bg-01.refarch.eng.vmware.com";
        final String vcoType = " vCenter Orchestrator App Server";
        Resource vco = factory.createResource(!CREATE_IF_NOT_EXIST, vcoType, vcoName, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vco, SIBLING));

        final String vcoTagType = "vCenter Orchestrator";
        Resource vcoTag = createLogialResource(factory, vRaApplicationName, vcoTagType);
        vcoTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));
        vco.addRelations(factory.createRelation(vcoTag, PARENT, CREATE_IF_NOT_EXIST));

        /**
         * Adding both vRAVA directly and the vRAVA load balancer because we
         * won't to which the FQDN we have points.
         */
        final String vravaType = "vRealize Automation Server";
        Resource vrava = factory.createResource(!CREATE_IF_NOT_EXIST, vravaType, vravaName, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vrava, SIBLING));

        final String vravaLoadBalancername = vravaName;
        final String vravaLoadBalancerType = "Manager Server Load Balancer Service";
        Resource vravaPossibleLoadBalancer = factory.createResource(!CREATE_IF_NOT_EXIST, vravaLoadBalancerType,
                vravaLoadBalancername, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vravaPossibleLoadBalancer, SIBLING, !CREATE_IF_NOT_EXIST));

        final String vravaTagType = "vRealize Automation Server";
        Resource vravaTag = createLogialResource(factory, vRaApplicationName, vravaTagType);
        vrava.addRelations(factory.createRelation(vravaTag, PARENT, CREATE_IF_NOT_EXIST));
        vravaTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        return iaasWebServer;
    }

    private static Resource createLogialResource(ObjectFactory objectFactory, String appName, String type) {
        return createLogicalResource(objectFactory, type, VRAUtils.getFullResourceName(type, appName));
    }

    private static Resource createLogicalResource(ObjectFactory objectFactory, String iassWebServerTagType,
            String iassWebServerTagName) {
        return objectFactory.createResource(CREATE_IF_NOT_EXIST, iassWebServerTagType, iassWebServerTagName,
                LOGICAL, ResourceSubType.TAG);
    }

    private static Resource getVraAppTag(ObjectFactory factory, String vRaApplicationName, String vraAppTagType) {
        final String vraAppTagName = VRAUtils.getFullResourceName(vraAppTagType, vRaApplicationName);
        Identifier vraAppTagIdentifier = factory.createIdentifier("application.name", vRaApplicationName);
        Resource vraAppTagResource = createLogicalResource(factory, vraAppTagType, vraAppTagName);
        vraAppTagResource.addIdentifiers(vraAppTagIdentifier);
        return vraAppTagResource;
    }

    private String executeXMLQuery(String path, File xmlFile) {
        String res = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = (Document) builder.parse(xmlFile);

            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xpath = xFactory.newXPath();

            res = xpath.evaluate(path, doc);
        } catch (Exception ex) {
            log.debug(ex, ex);
        }
        return res;
    }

    private String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ex) {
            log.debug("[getCanonicalPath] " + ex, ex);
            return file.getAbsolutePath();
        }

    }
}
