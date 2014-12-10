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
                String vRealizeAutomationVA = executeXMLQuery("//repository/@store", webCfg);
                vRealizeAutomationVA = vRealizeAutomationVA.replaceAll("\\w*://([^:/]*).*", "$1");
                log.debug("[newServerResource] vRealizeAutomationVA=" + vRealizeAutomationVA);

                String model = VRAUtils.marshallResource(getIaaSWebServerRelationsModel(vRealizeAutomationVA, vRAIaaSWebLoadBalancer));
                ConfigResponse c = new ConfigResponse();
                c.setValue(PROP_EXTENDED_REL_MODEL, new String(Base64.encodeBase64(model.getBytes())));

                // do not remove, why? please don't ask.
                server.setProductConfig(server.getProductConfig());
                server.setCustomProperties(c);

            }
        }
        return server;
    }

    public static Resource getIaaSWebServerRelationsModel(String vravaName, String vRAIaaSWebLoadBalancer) {
        ObjectFactory factory = new ObjectFactory();

        final String vRaApplicationName = "${application.name}";
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

        final String lbResourceType = "vRealize Automation IaaS Web Load Balancer Service";
        Resource loadBalancer = getLoadBalancer(factory, vRaApplicationName, lbResourceType);
        iaasWebServer.addRelations(factory.createRelation(loadBalancer, SIBLING));

        final String lbTagType = "vRealize Automation IaaS Web Load Balancer Service";
        Resource loadBalancerTag = createLogialResource(factory, vRAIaaSWebLoadBalancer, lbTagType);
        loadBalancer.addRelations(factory.createRelation(loadBalancerTag, PARENT));
        loadBalancerTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));

        final String lbsTagType = "Load Balancer";
        Resource loadBalancerSuperTag = createLogialResource(factory, vRaApplicationName, lbsTagType);
        loadBalancerTag.addRelations(factory.createRelation(loadBalancerSuperTag, PARENT));

        final String vcoName = "lg-vco.ra.local";
        final String vcoType = "vCO App Server";
        Resource vco = factory.createResource(!CREATE_IF_NOT_EXIST, vcoType, vcoName, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vco, SIBLING));

        final String vcoTagType = "vCO";
        Resource vcoTag = createLogialResource(factory, vRaApplicationName, vcoTagType);
        vcoTag.addRelations(factory.createRelation(vraAppTagResource, PARENT));
        vco.addRelations(factory.createRelation(vcoTag, PARENT, CREATE_IF_NOT_EXIST));

        final String vravaType = "vRealize Automation Server";
        Resource vrava = factory.createResource(!CREATE_IF_NOT_EXIST, vravaType, vravaName, SERVER);
        iaasWebServer.addRelations(factory.createRelation(vrava, SIBLING));

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

    private static Resource getLoadBalancer(ObjectFactory factory, String vRaApplicationName, String lbResourceType) {
        final String lbResourceName = VRAUtils.getFullResourceName(lbResourceType, vRaApplicationName);
        return factory.createResource(Boolean.FALSE, lbResourceType, lbResourceName, SERVER);
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
